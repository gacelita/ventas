(ns ventas.database
  (:require
   [clojure.core.async :as core.async :refer [<! >! chan go go-loop]]
   [clojure.spec.alpha :as spec]
   [clojure.string]
   [clojure.test.check.generators :as gen]
   [datomic.api :as d]
   [io.rkn.conformity :as conformity]
   [mount.core :refer [defstate]]
   [slingshot.slingshot :refer [throw+]]
   [taoensso.timbre :as timbre]
   [ventas.config :as config]
   [ventas.database.generators :as db.generators])
  (:import
   [java.util.concurrent ExecutionException]
   [datomic Datom]
   [datomic.query EntityMap]))

(def ^:dynamic db)

(defn start-db! []
  (let [url (config/get :database :url)]
    (timbre/info (str "Starting database, URL: " url))
    (try
      (d/create-database url)
      (d/connect url)
      (catch ExecutionException e
        (throw (ex-info "Error connecting (database offline?)" {}))))))

(defn stop-db! [db]
  (timbre/info "Stopping database"))

(defstate db :start (start-db!) :stop (stop-db! db))

(defn q
  "q wrapper"
  ([query]
   (q query []))
  ([query sources]
   (apply d/q query (concat [(d/db db)] sources))))

(defn pull
  "pull wrapper"
  [& args]
  (apply d/pull (d/db db) args))

(defn transact
  "transact wrapper"
  [& args]
  (try
    @(apply d/transact db args)
    (catch Throwable e
      (throw+ {:type ::transact-exception :message (.getMessage e) :args args}))))

(defn history
  "history wrapper"
  [& args]
  (apply d/history (d/db db) args))

(defn ident
  "ident wrapper"
  [& args]
  (apply d/ident (d/db db) args))

(defn basis-t
  "Gets the last t"
  []
  (-> db d/db d/basis-t))

(defn log
  "log wrapper"
  []
  (d/log db))

(defn index-range
  "index-range wrapper"
  [attrid start end]
  (d/index-range (d/db db) attrid start end))

(defn transaction-log
  "Gets the list of all transactions"
  []
  (get-in (log) [:tail :txes]))

(defn transaction
  "Gets a transaction by t"
  [t]
  (-> (log) (d/tx-range t nil) first))

(defn entity
  "entity wrapper"
  [& args]
  (apply d/entity (d/db db) args))

(defn datoms
  "datoms wrapper"
  [& args]
  (apply d/datoms (d/db db) args))

(defn tx-report-queue
  "tx-report-queue wrapper"
  [& args]
  (apply d/tx-report-queue db args))

(defn resolve-tempid
  "resolve-tempid wrapper"
  [& args]
  (apply d/resolve-tempid (d/db db) args))

(defn retract-entity
  "Retract an entity by eid"
  [eid]
  (transact [[:db.fn/retractEntity eid]]))

(defn tempid
  "tempid wrapper"
  []
  (d/tempid :db.part/user))

(defn normalize-ref [ref]
  (cond
    (vector? ref) (:db/id (entity ref))
    (:db/id ref) (:db/id ref)
    :default ref))

(defn datom->map
  [^Datom datom]
  (let [e  (.e datom)
        a  (.a datom)
        v  (.v datom)
        tx (.tx datom)
        added (.added datom)]
    {:e (or (ident e) e)
     :a (or (ident a) a)
     :v (or (and (= :db.type/ref (:db/valueType (entity a)))
                 (ident v))
            v)
     :tx (d/tx->t tx)
     :added added}))

(defn transaction->map [tx]
  (update tx :data #(map datom->map %)))

(defn rollback
  "Reassert retracted datoms and retract asserted datoms in a transaction,
  effectively 'undoing' the transaction."
  ([] (rollback (basis-t)))
  ([t]
   (let [tx (transaction t)
         tx-eid   (-> tx :t d/t->tx)
         new-datoms (->> (:data tx)
                          ; Remove transaction-metadata datoms
                         (remove #(= (:e %) tx-eid))
                          ; Invert the datoms add/retract state.
                         (map #(do [(if (:added %) :db/retract :db/add) (:e %) (:a %) (:v %)]))
                          ; Reverse order of inverted datoms.
                         reverse)]
     @(transact new-datoms))))

(defn partitions
  "Gets the partitions of the database"
  []
  (q '[:find ?ident
       :where [:db.part/db :db.install/partition ?p]
              [?p :db/ident ?ident]]))

(defn EntityMaps->eids
  "EntityMap -> eid"
  [m]
  (into {}
        (for [[k v] m]
          [k (cond
               (instance? EntityMap v) (:db/id v)
               (set? v)
               (cond
                 (instance? EntityMap (first v)) (map :db/id v)
                 :else v)
               :else v)])))

(defn touch-eid
  "Touches an entity by eid"
  [eid]
  {:pre [eid]}
  (when-let [entity (entity eid)]
    (let [result (d/touch entity)]
      (-> (into {} result)
          (assoc :db/id (:db/id result))
          (EntityMaps->eids)))))

(defn schema
  "Gets the current database schema"
  []
  (let [system-ns #{"db" "db.alter" "db.sys" "db.type" "db.install" "db.part"
                    "db.lang" "fressian" "db.unique" "db.excise" "db.cardinality" "db.fn"}]
    (map touch-eid
         (sort (q '[:find [?ident ...]
                    :in $ ?system-ns
                    :where [?e :db/ident ?ident]
                           [(namespace ?ident) ?ns]
                           [((comp not contains?) ?system-ns ?ns)]
                           [_ :db.install/attribute ?e]]
                  [system-ns])))))

(defn attributes
  "Gets all attributes. This is a superset of the schema."
  []
  (map touch-eid
       (sort (q '[:find [?ident ...]
                  :where [?e :db/ident ?ident]
                         [(namespace ?ident) ?ns]
                         [_ :db.install/attribute ?e]]))))

(defn read-changes
  "Given a report from tx-report-queue and a query, gets the changes"
  [{:keys [db-after tx-data] :as report} query]
  (d/q query
       db-after
       tx-data))

(defn keyword->db-symbol [keyword]
  (symbol (str "?" (name keyword))))

(defn db-symbol->keyword [db-symbol]
  (keyword (subs (str db-symbol) 1)))

(defn kw->type [kw]
  {:pre [(keyword? kw)]}
  (keyword "schema.type" (name kw)))

(defn type->kw [type]
  {:pre [(keyword? type)]}
  (keyword (name type)))

(defn map->query [m]
  (vec (concat '(:find) (:find m)
               '(:in) (:in m)
               '(:where) (:where m))))

(defn- nice-query* [{:keys [find where in]}]
  (map->query {:find (remove nil? find)
               :in (concat ['$] (remove nil? in))
               :where where}))

(defn nice-query
  "Automates the :in argument and returns maps instead of vectors"
  [{:keys [find in where]}]
  {:pre [(sequential? find) (or (nil? in) (map? in)) (sequential? where)]}
  (let [in (or in {})
        where (or where [])
        find (vec find)
        query (nice-query* {:find find
                            :in (keys in)
                            :where where})]
    (map (fn [result]
           (into {}
                 (map-indexed
                  (fn [idx itm]
                    [(db-symbol->keyword (nth find idx)) itm])
                  result)))
         (q query (vals in)))))

(defn enum-values
  "Gets the values of a database enum
   Usage: (enum-values \"schema.type\")"
  [enum & {:keys [eids?]}]
  (nice-query {:find ['?ident (when eids? '?id)]
               :in {'?enum enum}
               :where '[[?id :db/ident ?ident]
                        [(name ?ident) ?value]
                        [(namespace ?ident) ?ns]
                        [(= ?ns ?enum)]]}))

(defn recreate
  "Recreates the database"
  []
  (let [url (config/get :database :url)]
    (timbre/info "Deleting database " url)
    (d/delete-database url)
    (timbre/info "Creating database " url)
    (d/create-database url)))

(defn ensure-conforms
  "conformity/ensure-conforms wrapper"
  [id migration]
  {:pre [(keyword? id) (coll? migration)]}
  (conformity/ensure-conforms db {id {:txes [migration]}}))

(spec/def :db/id number?)

(spec/def ::pull-eid (spec/keys :req [:db/id]))

(spec/def ::lookup-ref
  (spec/with-gen
    (spec/tuple keyword? some?)
    #(gen/tuple (db.generators/keyword-generator)
                (db.generators/keyword-generator))))

(defn lookup-ref? [v]
  (spec/valid? ::lookup-ref v))

(defn rename-ident! [old new]
  (transact
   [{:db/id old
     :db/ident new}]))

(defn explain-tx [tx]
  (let [{:keys [t data]} (transaction->map tx)
        type-datom (->> data
                        (filter #(= (:a %) :schema/type))
                        first)]
    {:t t
     :entity-id (:e type-datom)
     :entity-type (when type-datom
                    (keyword (name (:v type-datom))))
     :type (->> data
                (filter #(= (:a %) :event/kind))
                first
                :v)}))

(defn explain-txs [txs]
  (map explain-tx txs))

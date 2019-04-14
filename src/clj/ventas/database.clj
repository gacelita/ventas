(ns ventas.database
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string]
   [clojure.test.check.generators :as gen]
   [datomic.api :as d]
   [io.rkn.conformity :as conformity]
   [mount.core :refer [defstate]]
   [slingshot.slingshot :refer [throw+]]
   [ventas.config :as config]
   [ventas.database.generators :as db.generators]
   [ventas.utils :as utils]
   [perseverance.core :as p]
   [clojure.tools.logging :as log]
   [ventas.common.utils :as common.utils])
  (:import
   [datomic Datom Connection]
   [datomic.query EntityMap]
   [java.util.concurrent ExecutionException]
   [clojure.lang ExceptionInfo]))

(defn start-db! []
  (p/retriable
   {:catch [ExceptionInfo]
    :tag ::connect-to-db}
   (let [url (config/get :database :url)]
     (log/info (str "Starting database, URL: " url))
     (try
       (d/create-database url)
       (d/connect url)
       (catch ExecutionException _
         (throw+ {:type ::database-connection-error
                  :message "Error connecting (database offline?)"}))))))

(defn stop-db! [_]
  (log/info "Stopping database"))

(defstate conn
  :start
  (p/retry
   {:strategy (p/constant-retry-strategy 100 3)}
   (start-db!))
  :stop (stop-db! conn))

(defn ^:dynamic db []
  (d/db conn))

(defn connected? []
  (instance? Connection conn))

(defn q
  "q wrapper"
  ([query]
   (q query []))
  ([query sources]
   (apply d/q query (into [(db)] sources))))

(defn pull
  "pull wrapper"
  [& args]
  (apply d/pull (db) args))

(defn pull*
  "pull [*]"
  [eid]
  (pull '[*] eid))

(defn transact*
  "transact wrapper"
  [& args]
  (try
    @(apply d/transact conn args)
    (catch Throwable e
      (throw+ {:type ::transact-exception
               :exception-message (.getMessage e)
               :args args}))))

(def ^:dynamic transact transact*)

(defn history
  "history wrapper"
  [& args]
  (apply d/history (db) args))

(defn ident
  "ident wrapper"
  [& args]
  (apply d/ident (db) args))

(defn basis-t
  "Gets the last t"
  []
  (-> (db) (d/basis-t)))

(defn log
  "log wrapper"
  []
  (d/log conn))

(defn index-range
  "index-range wrapper"
  [attrid start end]
  (d/index-range (db) attrid start end))

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
  (apply d/entity (db) args))

(defn datoms
  "datoms wrapper"
  [& args]
  (apply d/datoms (db) args))

(defn tx-report-queue
  "tx-report-queue wrapper"
  [& args]
  (apply d/tx-report-queue conn args))

(defn resolve-tempid
  "resolve-tempid wrapper"
  [& args]
  (apply d/resolve-tempid (d/db conn) args))

(defn retract-entity
  "Retract an entity by eid"
  [eid]
  (transact [[:db.fn/retractEntity eid]]))

(defn tempid
  "tempid wrapper"
  []
  (d/tempid :db.part/user))

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

(defn- EntityMap->eid
  [v]
  (cond
    (instance? EntityMap v) (:db/id v)
    (set? v) (map EntityMap->eid v)
    :else v))

(defn EntityMaps->eids
  "EntityMap -> eid"
  [m]
  (common.utils/map-vals EntityMap->eid m))

(defn touch-eid
  "Touches an entity by eid"
  [eid]
  {:pre [eid]}
  (when-let [entity (entity eid)]
    (let [result (d/touch entity)]
      (-> (into {} result)
          (assoc :db/id (:db/id result))
          (EntityMaps->eids)))))

(defn normalize-ref
  "Normalizes a database reference.
   {:db/id 17592186045466} -> 123
   :schema.type/product -> 17592186045466
   [:product/keyword :test-product] -> 17592186045940"
  [ref]
  (cond
    (or (vector? ref) (keyword? ref)) (:db/id (entity ref))
    (:db/id ref) (:db/id ref)
    :default ref))

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

(defn map->query [m]
  (utils/into-n
   [:find] (:find m)
   [:in] (:in m)
   [:where] (:where m)))

(defn- nice-query* [{:keys [find where in]}]
  (map->query {:find (remove nil? find)
               :in (into ['$] (remove nil? in))
               :where where}))

(defn nice-query
  "Automates the :in argument and returns maps instead of vectors"
  [{:keys [find in where]} & [explicit-db]]
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
         (apply d/q query (into [(or explicit-db (db))]
                                (vals in))))))

(defn nice-query-one
  "(first (nice-query))"
  [{:keys [find in where] :as args}]
  (first (nice-query args)))

(defn nice-query-attr
  "Returns the only attribute of the only row"
  [{:keys [find in where] :as args}]
  (when-let [entry (first (nice-query-one args))]
    (val entry)))

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

(defn delete
  "Wrapper for d/delete-database"
  []
  (let [url (config/get :database :url)]
    (log/info "Deleting database " url)
    (d/delete-database url)))

(defn create
  "Wrapper for d/create-database"
  []
  (let [url (config/get :database :url)]
    (log/info "Creating database " url)
    (d/create-database url)))

(defn recreate
  "Recreates the database"
  []
  (delete)
  (create))

(defn ensure-conforms
  "conformity/ensure-conforms wrapper"
  [id migration]
  {:pre [(keyword? id) (or (coll? migration) (fn? migration))]}
  (conformity/ensure-conforms
   conn
   {id (if (fn? migration)
         {:txes-fn migration}
         {:txes    [migration]})}))

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

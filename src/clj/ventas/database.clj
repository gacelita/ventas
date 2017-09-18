(ns ventas.database
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [clojure.pprint :as p]
    [datomic.api :as d]
    [buddy.hashers :as hashers]
    [mount.core :as mount :refer [defstate]]
    [ventas.config :refer [config]]
    [ventas.util :as util]
    [slingshot.slingshot :refer [throw+ try+]]
    [clojure.spec.alpha :as spec]
    [clojure.spec.test.alpha :as stest]
    [clojure.test.check.generators :as gen]
    [com.gfredericks.test.chuck.generators :as gen']
    [taoensso.timbre :as timbre :refer (trace debug info warn error)])
  (:import [java.io File]))

(defn start-db! []
  (let [url (get-in config [:database :url])]
    (util/print-info (str "Starting database, URL: " url))
    (try
      (d/connect (get-in config [:database :url]))
      (catch java.util.concurrent.ExecutionException e
        (throw (ex-info "Error connecting (database offline?)" {}))))))

(defn stop-db! [db]
  (util/print-info "Stopping database"))

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
  (apply d/pull (concat [(d/db db)] args)))

(defn transact
  "transact wrapper"
  [& args]
  (try
    @(apply d/transact db args)
    (catch Exception e
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

(defn transaction
  "Gets a transaction by t"
  [t]
  (-> db d/log (d/tx-range t nil) first))

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
  @(transact [[:db.fn/retractEntity eid]]))

(defn datom->map
  [^datomic.Datom datom]
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

(defn touch-eid
  "Touches an entity by eid"
  [eid]
  (into {} (d/touch (entity eid))))

(defn EntityMaps->eids
  "EntityMap -> eid"
  [m]
  (into {}
    (for [[k v] m]
      [k (cond
           (instance? datomic.query.EntityMap v) (:db/id v)
           (set? v)
             (cond
               (instance? datomic.query.EntityMap (first v)) (map :db/id v)
               :else v)
           :else v)])))

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

(defn enum-values
  "Gets the values of a database enum
   Usage: (enum-values \"schema.type\")"
  [enum]
  (into #{}
        (map first
             (q '[:find ?ident
                  :in $ ?enum
                  :where [?id :db/ident ?ident]
                  [(name ?ident) ?value]
                  [(namespace ?ident) ?ns]
                  [(= ?ns ?enum)]]
                [enum]))))

(defn read-changes
  "Given a report from tx-report-queue and a query, gets the changes"
  [{:keys [db-after tx-data] :as report} query]
  (d/q query
       db-after
       tx-data))

(defn keyword->db-symbol [keyword]
  (symbol (str "?" (name keyword))))

(defn map->query [m]
  (vec (concat '(:find) (:find m)
               '(:in) (:in m)
               '(:where) (:where m))))

(defn- filtered-query* [wheres filters]
  (map->query {:in (concat ['$] (remove nil? (map keyword->db-symbol (keys filters))))
               :where wheres
               :find '(?id)}))

(defn filtered-query
  "Filtered query.
   Usage: (filtered-query (quote ([?id :user/email ?email])) {:email \"some-email@example.com\"})"
  ([] (filtered-query '() {}))
  ([wheres] (filtered-query wheres {}))
  ([wheres filters]
   (let [query (filtered-query* wheres filters)]
     (q query (vals filters)))))

(defn recreate
  "Recreates the database"
  []
  (let [url (get-in config [:database :url])]
    (info "Deleting database " url)
    (d/delete-database url)
    (info "Creating database " url)
    (d/create-database url)))

(ns ventas.database
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [clojure.pprint :as p]
    [datomic.api :as d]
    [adi.core :as adi]
    [buddy.hashers :as hashers]
    [mount.core :as mount :refer [defstate]]
    [ventas.config :refer [config]]
    [ventas.util :as util :refer [print-info]]
    [slingshot.slingshot :refer [throw+ try+]]
    [clojure.spec :as s]
    [clojure.spec.test :as stest]
    [clojure.test.check.generators :as gen]
    [com.gfredericks.test.chuck.generators :as gen']
    [taoensso.timbre :as timbre :refer (trace debug info warn error)])
  (:import [java.io File]))

;; Require all client

(defn start-db! []
  (let [url (get-in config [:database :url])]
    (print-info (str "Starting database, URL: " url))
    (d/connect (get-in config [:database :url]))))
(defn stop-db! [db]
  (print-info "Stopping database"))
(defstate db :start (start-db!) :stop (stop-db! db))

(defn entity-spec
  "Checks that an entity complies with its spec"
  [data]
  (if (s/valid? (:schema/type data) data)
     data
     (throw+ {:type ::spec-invalid :message (s/explain (:schema/type data) data)})))

(defn generate-1
  "Generate one sample of a given spec"
  [spec]
  (gen/generate (s/gen spec)))

(defn generate-n
  "Generates n samples of given spec"
  [spec n]
  (let [generator (s/gen spec)]
    (map (fn [_] (gen/generate generator)) (range n))))

(defn basis-t
  "Gets the last t"
  []
  (-> db d/db d/basis-t))

(defn get-transaction
  "Gets a transaction by t"
  [t]
  (-> db d/log (d/tx-range t nil) first))

(defn datom->map
  [^datomic.Datom datom]
  (let [e  (.e datom)
        a  (.a datom)
        v  (.v datom)
        tx (.tx datom)
        added (.added datom)]
    {:e (or (d/ident (d/db db) e) e)
     :a (or (d/ident (d/db db) a) a)
     :v (or (and (= :db.type/ref (:db/valueType (d/entity (d/db db) a)))
              (d/ident (d/db db) v))
         v)
     :tx (d/tx->t tx)
     :added added}))

(defn rollback
  "Reassert retracted datoms and retract asserted datoms in a transaction,
  effectively 'undoing' the transaction."
  ([] (rollback (basis-t)))
  ([t]
    (let [tx (get-transaction t)
          tx-eid   (-> tx :t d/t->tx)
          new-datoms (->> (:data tx)
                          ; Remove transaction-metadata datoms
                          (remove #(= (:e %) tx-eid))
                          ; Invert the datoms add/retract state.
                          (map #(do [(if (:added %) :db/retract :db/add) (:e %) (:a %) (:v %)]))
                          ; Reverse order of inverted datoms.
                          reverse)]
      @(d/transact db new-datoms))))

(defn get-partitions
  "Gets the partitions of the database"
  []
  (d/q '[:find ?ident :where [:db.part/db :db.install/partition ?p]
                             [?p :db/ident ?ident]]
       (d/db db)))

(defn entity-dates
  "First and last dates associated with an eid"
  [eid]
  (first (d/q '[:find (min ?tx-time) (max ?tx-time)
                :in $ ?e
                :where
                  [?e _ _ ?tx _]
                  [?tx :db/txInstant ?tx-time]]
            (d/history (d/db db)) eid)))

(defn touch-eid
  "Touches an entity by EID"
  [eid]
  (into {} (d/touch (d/entity (d/db db) eid))))

(defn get-entity
  "Gets an entity with ID and dates"
  [eid]
  (let [dates (entity-dates eid)]
    (-> (touch-eid eid)
        (assoc :id eid)
        (assoc :created-at (get dates 0))
        (assoc :updated-at (get dates 1)))))

(defn EntityMaps->eids
  "EntityMap -> eid"
  [m]
  (into {}
    (for [[k v] m]
      [k (if (instance? datomic.query.EntityMap v) (:db/id v) v)])))

(defn process-db-entity
  "Processes an entity from the database"
  [data]
  (-> data
      (util/dequalify-keywords)
      (EntityMaps->eids)))

(defn process-transaction
  "Returns an entity from a transaction"
  [type tx tempid]
  (-> (d/resolve-tempid (d/db db) (:tempids tx) tempid)
      (get-entity)
      (process-db-entity)))

(defn get-schema
  "Gets the current database schema"
  []
  (let [system-ns #{"db" "db.alter" "db.sys" "db.type" "db.install" "db.part" 
                    "db.lang" "fressian" "db.unique" "db.excise" "db.cardinality" "db.fn"}]
    (map touch-eid
      (sort (d/q '[:find [?ident ...]
                   :in $ ?system-ns
                   :where [?e :db/ident ?ident]
                          [(namespace ?ident) ?ns]
                          [((comp not contains?) ?system-ns ?ns)]
                          [_ :db.install/attribute ?e]]
                  (d/db db) system-ns)))))

(defn get-enum-values
  "Gets the values of a database enum
   Usage: (get-enum-values \"schema.type\""
  [enum]
  (d/q '[:find ?id ?ident ?value
         :in $ ?enum
         :where [?id :db/ident ?ident]
                [(name ?ident) ?value]
                [(namespace ?ident) ?ns]
                [(= ?ns ?enum)]] (d/db db) enum))

(defn retract-entity
  "Retract an entity by eid"
  [eid]
  @(d/transact db [[:db.fn/retractEntity eid]]))

(defn pull
  "Small pull helper"
  [& args]
  (apply d/pull (concat [(d/db db)] args)))

(defn read-changes
  "Given a report from tx-report-queue and a query, gets the changes"
  [{:keys [db-after tx-data] :as report} query]
  (d/q query
       db-after
       tx-data))

(defn tx-report-queue [] (d/tx-report-queue db))

(defn keyword->db-symbol [keyword]
  (symbol (str "?" (name keyword))))

(defn map->query [m]
  (vec (concat '(:find) (:find m)
               '(:in) (:in m)
               '(:where) (:where m))))

(defn filtered-query
  "Filtered query.
   Usage: (filtered-query (quote ([?id :user/email ?email])) {:email \"some-email@example.com\"})"
  ([] (filtered-query '() {}))
  ([wheres] (filtered-query wheres {}))
  ([wheres filters]
   (let [ins (concat ['$] (remove nil? (map keyword->db-symbol (keys filters))))
         query (map->query {:in ins :where wheres :find '(?id)})]
     (apply d/q (concat [query (d/db db)] (vals filters))))))

(defn entity-filtered-query
  "@todo Refactor me"
  ([type filters]
    (entity-filtered-query type (map (fn [[k v]] [ '?id (if (namespace k) k (util/qualify-keyword k type)) (if (= v :any) '_ (keyword->db-symbol k))]) filters) filters))
  ([type wheres filters]
    (filtered-query wheres filters)))

(declare entity-query)

(defmulti entity-preseed (fn entity-preseed [type data] type))
(defmethod entity-preseed :default [type data] data)

(defmulti entity-precreate (fn entity-precreate [data] (keyword (name (:schema/type data)))))
(defmethod entity-precreate :default [data] data)

(defmulti entity-postcreate (fn entity-postcreate [type entity] type))
(defmethod entity-postcreate :default [type entity] true)

(defmulti entity-predelete (fn entity-predelete [type entity] type))
(defmethod entity-predelete :default [type entity] true)

(defmulti entity-postdelete (fn entity-postdelete [type entity] type))
(defmethod entity-postdelete :default [type entity] true)

(defmulti entity-preupdate (fn entity-preupdate [type entity params] type))
(defmethod entity-preupdate :default [type entity params] true)

(defmulti entity-postupdate (fn entity-postupdate [type entity params] type))
(defmethod entity-postupdate :default [type entity params] true)

(defmulti entity-postquery (fn entity-postquery [entity] (keyword (name (:type entity)))))
(defmethod entity-postquery :default [entity] entity)

(defmulti entity-postseed (fn entity-postseed [entity] (keyword (name (:type entity)))))
(defmethod entity-postseed :default [entity] entity)

(defmulti entity-json
  "This should return a map to be JSON-encoded and most likely
   sent to the client. Useful for hiding attributes, resolving EIDs
   or formatting attributes"
  (fn [entity] (keyword (name (:type entity)))))
(defmethod entity-json :default [entity]
  (-> entity
    (dissoc :type)))

(defmulti entity-query
  "Multimethod for querying entities"
  (fn entity-query
    ([type] type)
    ([type params] type)
    ([type wheres params] type)))

(defmethod entity-query :default
  ([type] (entity-query type {:schema/type (keyword "schema.type" (name type))}))
  ([type params]
    (let [results (entity-filtered-query type params)]
      (map (fn [[eid]] (entity-postquery (process-db-entity (get-entity eid)))) results)))
  ([type wheres params]
    (let [results (entity-filtered-query type wheres params)]
      (map (fn [[eid]] (entity-postquery (process-db-entity (get-entity eid)))) results))))

(defn entity-find
  "Finds entities by eid"
  [eid]
  (process-db-entity (get-entity eid)))

(defmulti entity-create
  "Multimethod for creating entities"
  (fn entity-create [type params] type))

(defmethod entity-create :default [type params]
  (let [tempid (d/tempid :db.part/user)
        data
          (-> (util/qualify-map-keywords (util/filter-empty-vals params) type)
              (assoc :db/id tempid)
              (assoc :schema/type (keyword "schema.type" (name type)))
              entity-precreate
              entity-spec)
        tx @(d/transact db [data])
        entity (process-transaction type tx tempid)]
    (entity-postcreate type entity)
    entity))

(defprotocol EntityType
  "Protocol for getting the :db.type of an entity
   Example usage: (entity-type user)"
  (entity-type [this]))

(defprotocol EntityUpdate
  "Protocol for updating entities
   Example usage: (entity-update user {:email \"new-email@example.com\"})"
  (entity-update [this params]))

(defprotocol EntityDelete
  "Protocol for deleting entities
   Example usage: (entity-delete user)"
  (entity-delete [this]))

(extend-type Object
  EntityType
    ;; By default, a ventas.database/User object gets the type ":user"
    (entity-type [this]
      (-> this class .getSimpleName clojure.string/lower-case keyword))
  EntityUpdate
    ;; Default update implementation.
    ;; Executes the preupdate method for the type, transacts the update, and executes the postupdate method for the type
    ;; postupdate method exists.
    (entity-update [this params]
      (when (nil? (:id this))
        (throw+ {:type ::invalid-update :entity this :message "The entity needs to have an ID in order to be updated"}))
      (entity-preupdate (entity-type this) this params)
      @(d/transact db [(assoc (util/qualify-map-keywords params (entity-type this)) :db/id (:id this))])
      (entity-postupdate (entity-type this) this params)
      (entity-find (:id this)))
  EntityDelete
    ;; Default delete implementation.
    ;; Executes the predelete method for the type, retracts the entity, and executes the postdelete method for the type
    (entity-delete [this]
      (when (nil? (:id this))
        (throw+ {:type ::invalid-deletion :entity this :message "The entity needs to have an ID in order to be deleted"}))
      (entity-predelete (entity-type this) this)
      (retract-entity (:id this))
      (entity-postdelete (entity-type this) this)
      (:id this)))

(defn entity-upsert
  "Entity upsert. Calls entity-update if necessary, entity-create otherwise"
  [type data]
  (if (:id data)
    (entity-update (entity-find (:id data)) (dissoc data :id))
    (entity-create type data)))

(defn seed-type
  "Seeds the database with n entities of a type"
  [type n]
  (info "Seeding " type)
  (doseq [entity-data (generate-n (keyword "schema.type" (name type)) n)]
    (let [entity-data (entity-preseed type entity-data)
          entity (entity-create type entity-data)]
      (entity-postseed entity))))

(defn seed
  "Seeds the database with sample data"
  []
  (seed-type :tax 10)
  (seed-type :file 10)
  (seed-type :brand 10)
  (seed-type :configuration 20)
  (seed-type :attribute 10)
  (seed-type :attribute-value 10)
  (seed-type :category 10)
  (seed-type :product 10)
  (seed-type :product-variation 10))




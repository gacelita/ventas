(ns ventas.database.entity
  (:refer-clojure :exclude [find type update])
  (:require [ventas.database :as db]
            [clojure.core :as clj]
            [taoensso.timbre :as timbre :refer [trace debug info warn error]]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.spec.alpha :as spec]
            [datomic.api :as d]
            [ventas.util :as util]
            [ventas.events :as events]
            [clojure.core.async :refer [go <! go-loop]]
            [ventas.database.schema :as schema]))

(defn is-entity? [entity]
  (contains? (set (keys entity)) :schema/type))

(spec/def ::entity-type
  (spec/keys :opt-un [::attributes
                      ::filter-json
                      ::filter-seed
                      ::filter-transact
                      ::filter-update
                      ::before-seed
                      ::before-transact
                      ::before-delete
                      ::after-seed
                      ::after-transact
                      ::after-delete]))

(defonce registered-types (atom {}))

(defn register-type!
  "Registers an entity type
   Example: (register-entity-type! :user)"
  [kw & [m]]
  {:pre [(keyword? kw) (or (nil? m)
                           (and (map? m) (util/check ::entity-type m)))]}
  (let [m (or m {})]
    (schema/register-migration!
     [{:db/ident (db/kw->type kw)}])
    (schema/register-migration!
     (or (:attributes m) []))
    (swap! registered-types assoc kw m)))

(defn types
  "Returns all types"
  []
  (keys @registered-types))

(defn type-exists?
  [type]
  {:pre [(keyword? type)]}
  (not (nil? (get @registered-types type))))

(defonce entity-fixtures (atom {}))

(defn register-fixtures!
  "Registers the fixtures for a type"
  [kw fixtures]
  {:pre [(keyword? kw) (coll? fixtures)]}
  (swap! entity-fixtures assoc kw fixtures))

(defn fixtures
  "Gets the fixtures of a type"
  [kw]
  (map (fn [fixture]
         (assoc fixture :schema/type (db/kw->type kw)))
       (get @entity-fixtures kw)))

(defn type
  "Returns the type of an entity"
  [entity]
  {:pre [(is-entity? entity)]}
  (debug entity)
  (keyword (name (:schema/type entity))))

(defn type-fns
  "Returns the functions of an entity type"
  [type]
  (get @registered-types type))

(def default-type
  {:attributes []
   :filter-json identity
   :filter-seed identity
   :filter-transact identity
   :filter-update (fn [entity data] data)
   :before-seed (fn [_] true)
   :before-transact (fn [_] true)
   :before-delete (fn [_] true)
   :after-seed (fn [_] true)
   :after-transact (fn [_] true)
   :after-delete (fn [_] true)})

(defn- call-type-fn [kw entity & args]
  (let [type-fns (type-fns (type entity))
        type-fn (if (kw type-fns)
                   (kw type-fns)
                   (kw default-type))]
    (apply type-fn entity args)))

(defn filter-json [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-json entity))

(defn filter-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-seed entity))

(defn filter-transact [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-transact entity))

(defn filter-update [entity data]
  {:pre [(is-entity? entity) (map? data)]}
  (call-type-fn :filter-update entity data))

(defn before-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-seed entity))

(defn before-transact [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-transact entity))

(defn before-delete [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-delete entity))

(defn after-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-seed entity))

(defn after-transact [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-transact entity))

(defn after-delete [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-delete entity))

(defn spec
  "Checks that an entity complies with its spec"
  [entity]
  {:pre [(type-exists? (type entity))]}
  (util/check (:schema/type entity) entity))

(defn find
  "Finds an entity by eid"
  [eid]
  {:pre [(number? eid)]}
  (let [db-entity (db/touch-eid eid)]
    (when (seq db-entity)
      (-> db-entity
          (assoc :db/id eid)
          (db/EntityMaps->eids)))))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (-> (db/resolve-tempid (:tempids tx) tempid)
      (find)))

(defn transact
  "Transacts an entity"
  [pre-entity]
  {:pre [(spec pre-entity)]}
  (before-transact pre-entity)
  (let [tempid (d/tempid :db.part/user)
        pre-entity (-> pre-entity
                       (assoc :db/id tempid)
                       (filter-transact))
        tx (db/transact [pre-entity])
        entity (transaction->entity tx tempid)]
    (after-transact entity)
    entity))

(defn create
  "Creates an entity and transacts it.
   Example usage:
   (create :user {:name `Joel` :email `test@test.com`})"
  [type attributes]
  (let [entity
        (-> (util/qualify-map-keywords (util/filter-empty-vals attributes) type)
            (assoc :schema/type (db/kw->type type)))]
    (transact entity)))

(defn to-json
  "Transforms an entity into a stripped map, suitable for sending to the outside"
  [entity]
  (-> entity
      (filter-json)
      (dissoc :type)
      (util/dequalify-keywords)))

(defn from-json
  "Inverse of to-json"
  [attributes]
  {:pre [(map? attributes)]}
  (let [id (:id attributes)
        type (:type attributes)]
    (-> attributes
        (dissoc :id)
        (dissoc :type)
        (util/qualify-map-keywords type)
        (assoc :schema/type type)
        (assoc :db/id id))))

(defn update
  "Takes an eid and a list of attributes to transact.
   Example usage:
   (update 1234567 {:name `Other name`})"
  [eid attributes]
  {:pre [(map? attributes) (number? eid)]}
  (let [entity (find eid)
        entity-type (type entity)
        data (as-> attributes data
                   (filter-update entity data)
                   (dissoc data :id)
                   (dissoc data :type)
                   (util/qualify-map-keywords data entity-type)
                   (assoc data :db/id (:db/id entity))
                   (assoc data :schema/type (db/kw->type entity-type)))]
    (db/transact [data])
    (find eid)))

(defn delete [eid]
  "Deletes an entity by eid"
  (let [entity (find eid)]
    (before-delete entity)
    (db/retract-entity eid)
    (after-delete entity)))

(defn upsert
  "Like create, but if the list of attributes contains an id, it is used as
   the argument for `update`"
  [type {:keys [id] :as attributes}]
  {:pre [(keyword? type) (map? attributes)]}
  (if id
    (update id attributes)
    (create type attributes)))

(defn dates
  "First and last dates associated with an eid"
  [eid]
  (let [query '[:find (min ?tx-time) (max ?tx-time)
                :in $ ?e
                :where [?e _ _ ?tx _]
                       [?tx :db/txInstant ?tx-time]]
        result (first (d/q query (db/history) eid))]
    {:created-at (get result 0)
     :updated-at (get result 1)}))

(defn- filters->wheres [type filters]
  (map (fn [[attribute value]]
         (let [attribute (if (namespace attribute) attribute (util/qualify-keyword attribute type))
               value (if (= value :any) '_ (db/keyword->db-symbol attribute))]
           ['?id attribute value]))
       filters))

(defn query
  "Performs a high-level query."
  ([type]
   (query type {:schema/type (db/kw->type type)}))
  ([type filters]
   (map #(find (first %))
        (db/filtered-query
         (filters->wheres type filters)
         (zipmap (map db/keyword->db-symbol (keys filters))
                 (vals filters))))))

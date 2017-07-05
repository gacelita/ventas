(ns ventas.database.entity
  (:refer-clojure :exclude [find type update])
  (:require [ventas.database :as db]
            [taoensso.timbre :as timbre :refer (trace debug info warn error)]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.spec :as s]
            [datomic.api :as d]
            [ventas.util :as util]))

(defmulti preseed (fn preseed [type data] type))
(defmethod preseed :default [type data] data)

(defmulti precreate (fn precreate [data] (keyword (name (:schema/type data)))))
(defmethod precreate :default [data] data)

(defmulti postcreate (fn postcreate [type entity] type))
(defmethod postcreate :default [type entity] true)

(defmulti predelete (fn predelete [type entity] type))
(defmethod predelete :default [type entity] true)

(defmulti postdelete (fn postdelete [type entity] type))
(defmethod postdelete :default [type entity] true)

(defmulti preupdate (fn preupdate [type entity params] type))
(defmethod preupdate :default [type entity params] true)

(defmulti postupdate (fn postupdate [type entity params] type))
(defmethod postupdate :default [type entity params] true)

(defmulti postquery (fn postquery [entity] (keyword (name (:type entity)))))
(defmethod postquery :default [entity] entity)

(defmulti postseed (fn postseed [entity] (keyword (name (:type entity)))))
(defmethod postseed :default [entity] entity)

(defn spec
  "Checks that an entity complies with its spec"
  [data]
  (if (s/valid? (:schema/type data) data)
    data
    (throw+ {:type ::spec-invalid :message (s/explain (:schema/type data) data)})))

(defn find
  "Finds an entity by eid"
  [eid]
  (-> (db/touch-eid eid)
      (assoc :id eid)
      (util/dequalify-keywords)
      (db/EntityMaps->eids)))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (-> (db/resolve-tempid (:tempids tx) tempid)
      (find)))

(defmulti create
          "Multimethod for creating entities"
          (fn create [type params] type))

(defmethod create :default [type params]
  (let [tempid (d/tempid :db.part/user)
        data
        (-> (util/qualify-map-keywords (util/filter-empty-vals params) type)
            (assoc :db/id tempid)
            (assoc :schema/type (keyword "schema.type" (name type)))
            precreate
            spec)
        tx @(db/transact [data])
        entity (transaction->entity tx tempid)]
    (postcreate type entity)
    entity))

(defmulti json
          "This should return a map to be JSON-encoded and most likely
           sent to the client. Useful for hiding attributes, resolving EIDs
           or formatting attributes"
          (fn [entity]
            (println entity)
            (keyword (name (:type entity)))))
(defmethod json :default [entity]
  (-> entity
      (dissoc :type)))

(defprotocol Entity
  (type [this])
  (update [this params])
  (delete [this]))

(extend-type Object
  Entity
  ;; By default, a ventas.database/User object gets the type ":user"
  (type [this]
    (-> this class .getSimpleName clojure.string/lower-case keyword))
  ;; Default update implementation.
  ;; Executes the preupdate method for the type, transacts the update, and executes the postupdate method for the type
  ;; postupdate method exists.
  (update [this params]
    (when (nil? (:id this))
      (throw+ {:type ::invalid-update :entity this :message "The entity needs to have an ID in order to be updated"}))
    (preupdate (type this) this params)
    @(db/transact [(assoc (util/qualify-map-keywords params (type this)) :db/id (:id this))])
    (postupdate (type this) this params)
    (find (:id this)))
  ;; Default delete implementation.
  ;; Executes the predelete method for the type, retracts the entity, and executes the postdelete method for the type
  (delete [this]
    (when (nil? (:id this))
      (throw+ {:type ::invalid-deletion :entity this :message "The entity needs to have an ID in order to be deleted"}))
    (predelete (type this) this)
    (db/retract-entity (:id this))
    (postdelete (type this) this)
    (:id this)))

(defn upsert
  "Entity upsert. Calls update if necessary, create otherwise"
  [type data]
  (if (:id data)
    (update (find (:id data)) (dissoc data :id))
    (create type data)))

(defmulti fixtures (fn [type] type))

(defmethod fixtures :default [type]
  [])

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

(defn filtered-query
  "@todo Refactor me"
  ([type filters]
   (filtered-query type (map (fn [[k v]] [ '?id (if (namespace k) k (util/qualify-keyword k type)) (if (= v :any) '_ (db/keyword->db-symbol k))]) filters) filters))
  ([type wheres filters]
   (db/filtered-query wheres filters)))

(defmulti query
  "Multimethod for querying entities"
  (fn query
    ([type] type)
    ([type params] type)
    ([type wheres params] type)))

(defmethod query :default
  ([type] (query type {:schema/type (keyword "schema.type" (name type))}))
  ([type params]
   (let [results (filtered-query type params)]
     (map (fn [[eid]] (postquery (find eid))) results)))
  ([type wheres params]
   (let [results (filtered-query type wheres params)]
     (map (fn [[eid]] (postquery (find eid))) results))))

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

(defmulti preupdate (fn preupdate [type entity data] type))
(defmethod preupdate :default [type entity data] data)

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
  (let [entity (db/touch-eid eid)]
    (when (seq entity)
      (-> entity
          (assoc :id eid)
          (util/dequalify-keywords)
          (db/EntityMaps->eids)))))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (-> (db/resolve-tempid (:tempids tx) tempid)
      (find)))

(defmulti create
          "Multimethod for creating entities"
          (fn [type params] type))

(defmethod create :default [type params]
  (let [tempid (d/tempid :db.part/user)
        data
        (-> (util/qualify-map-keywords (util/filter-empty-vals params) type)
            (assoc :db/id tempid)
            (assoc :schema/type (keyword "schema.type" (name type)))
            (precreate)
            (spec))
        tx @(db/transact [data])
        entity (transaction->entity tx tempid)]
    (postcreate type entity)
    entity))

(defmulti json
          "This should return a map to be JSON-encoded and most likely
           sent to the client. Useful for hiding attributes, resolving EIDs
           or formatting attributes"
          (fn [entity]
            (keyword (name (:type entity)))))

(defmethod json :default [entity]
  (-> entity
      (dissoc :type)))

(defmulti update (fn [entity new-data] (-> entity :type name)))

(defmethod update :default [entity data]
  (let [type (-> entity :type name)
        data (as-> data data
                   (preupdate type entity data)
                   (util/qualify-map-keywords data type)
                   (assoc data :db/id (:id entity)))]
    @(db/transact [data])
    (postupdate type entity data)
    (find (:id entity))))

(defmulti delete (fn [entity] (-> entity :type name)))

(defmethod delete :default [entity]
  (let [type (-> entity :type name)]
    (predelete type entity)
    (db/retract-entity (:id entity))
    (postdelete type entity)
    (:id entity)))

(defn upsert
  "Entity upsert. Calls update if necessary, create otherwise"
  [type data]
  (if (:id data)
    (update type (find (:id data)) (dissoc data :id))
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

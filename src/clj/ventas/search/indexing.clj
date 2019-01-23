(ns ventas.search.indexing
  (:require
   [ventas.common.utils :as common.utils]
   [ventas.database.entity :as entity]
   [ventas.entities.category :as entities.category]
   [ventas.utils :as utils]
   [ventas.search :as search]
   [ventas.search.schema :as search.schema]
   [ventas.database :as db]
   [mount.core :refer [defstate]]
   [clojure.set :as set]
   [clojure.tools.logging :as log]))

(defmulti transform-entity-by-type (fn [entity] (:schema/type entity)))

(defmethod transform-entity-by-type :schema.type/i18n [entity]
  (->> (entity/serialize entity)
       (utils/mapm (fn [[culture value]]
                     [(->> culture
                           entity/find
                           :i18n.culture/keyword)
                      value]))))

(defmethod transform-entity-by-type :schema.type/amount [entity]
  (:amount/value entity))

(defmethod transform-entity-by-type :schema.type/category [entity]
  (assoc entity :category/full-name
                (transform-entity-by-type
                 (entities.category/full-name-i18n (:db/id entity)))))

(defmethod transform-entity-by-type :schema.type/product [entity]
  (update entity :product/categories #(set (mapcat entities.category/get-parents %))))

(defmethod transform-entity-by-type :default [entity]
  entity)

(defn- ref->es [{:schema/keys [type] :as entity}]
  (if (contains? #{:schema.type/i18n
                   :schema.type/amount} type)
    (transform-entity-by-type entity)
    (:db/id entity)))

(defn- value->es [v]
  (cond
    (number? v)
    (if-let [entity (entity/find v)]
      (ref->es entity)
      v)
    :default v))

(defn- expand-i18n-entity [e [a v]]
  (if-not (map? v)
    (assoc e a v)
    (merge e
           (->> v
                (common.utils/map-keys
                 #(keyword (namespace a)
                           (str (name a) "__" (name %))))))))

(defn- db-id->document-id [e]
  (set/rename-keys e {:db/id :document/id}))

(defn index-entity [eid]
  (let [doc (->> (entity/find eid)
                 ;; Does transformations depending on the entity type
                 transform-entity-by-type
                 ;; Resolves value refs (currently i18n and amount)
                 (map (fn [[a v]]
                        [a (value->es v)]))
                 ;; Expands i18n entities, such that this:
                 ;; :product/name {:en_US "Shirt"
                 ;;                :es_ES "Camiseta"}
                 ;; turns into this:
                 ;; :product/name__en_US "Shirt"
                 ;; :product/name__es_ES "Camiseta"
                 (reduce expand-i18n-entity
                         {})
                 ;; Change :db/id to :document/id, as that's what the indexer expects
                 (db-id->document-id)
                 ;; product.taxonomy/keyword -> product__taxonomy/keyword
                 (common.utils/map-keys search.schema/ident->property))]
    (search/document->indexing-queue doc)))

(defn- indexable-types []
  (->> @ventas.database.entity/registered-types
       (filter (fn [[k v]]
                 (not (:component? v))))
       (keys)))

(defn reindex
  "Indexes everything"
  []
  (utils/swallow
   (search/remove-index))
  (search.schema/setup)
  (let [types (indexable-types)]
    (doseq [type types]
      (let [entities (entity/query type)]
        (doseq [{:db/keys [id]} entities]
          (index-entity id))))))

(defn- index-report [{:keys [tx-data]}]
  (let [types (->> (indexable-types)
                   (map name)
                   (set))
        eids (->> tx-data
                  (map db/datom->map)
                  (filter (fn [{:keys [e a]}]
                            (let [type (namespace a)]
                              (and (not= type "event")
                                   (contains? types type)))))
                  (map :e))]
    (doseq [eid eids]
      (index-entity eid))))

(defn start-tx-report-queue-loop! []
  (future
   (loop []
     (when-not (Thread/interrupted)
       (utils/interruptible-try
        (when-let [report (.take (db/tx-report-queue))]
          (index-report report)))
       (recur)))))

(defstate tx-report-queue-loop
  :start
  (do
    (log/info "Starting tx-report-queue loop")
    (start-tx-report-queue-loop!))
  :stop
  (do
    (log/info "Stopping tx-report-queue loop")
    (future-cancel tx-report-queue-loop)))
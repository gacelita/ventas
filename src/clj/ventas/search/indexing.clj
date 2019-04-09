(ns ventas.search.indexing
  (:require
   [ventas.common.utils :as common.utils]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]
   [ventas.search :as search]
   [ventas.search.schema :as search.schema]
   [ventas.database :as db]
   [mount.core :refer [defstate]]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [ventas.database.tx-processor :as tx-processor]))

(defmulti transform-entity-by-type
          "Transforms `entity` depending on its :schema/type.
           The result will be indexed in ES."
          (fn [entity] (keyword (name (:schema/type entity)))))

(defmethod transform-entity-by-type :default [entity]
  entity)

(defn- expand-i18n-entity
  "Deprecated"
  [e [a v]]
  (if-not (map? v)
    (assoc e a v)
    (merge e
           (->> v
                (common.utils/map-keys
                 #(keyword (namespace a)
                           (str (name a) "__" (name %))))))))

(defn- db-id->document-id [e]
  (set/rename-keys e {:db/id :document/id}))

(defn ident->property [ident]
  {:pre [(keyword? ident)]}
  (str/replace (str (namespace ident) "/" (name ident))
               "."
               "__"))

(defn- resolve-component-refs [entity]
  (let [attrs (entity/attributes-by-schema-kv entity :db/isComponent true)]
    (reduce (fn [acc attr]
              (let [value (get acc attr)]
                (if-let [subentity (and (number? value) (entity/find value))]
                  (assoc acc attr (transform-entity-by-type subentity))
                  acc)))
            entity
            attrs)))

(defn entity->doc [entity]
  (->> entity
       ;; Does transformations depending on the entity type
       transform-entity-by-type
       ;; Resolves component refs like :i18n and turns them into values
       resolve-component-refs
       ;; Changes :db/id to :document/id, as that's what the indexer expects
       (db-id->document-id)
       ;; product.taxonomy/keyword -> product__taxonomy/keyword
       (common.utils/map-keys ident->property)))

(defn index-entity [entity]
  (->> entity
       (entity->doc)
       (search/document->indexing-queue)))

(defn reindex-type [type]
  (doseq [entity (entity/query type)]
    (index-entity entity)))

(defn reindex
  "Indexes everything"
  []
  (utils/swallow
   (search/remove-index))
  (search.schema/setup!)
  (let [types (search/indexable-types)]
    (doseq [type types]
      (reindex-type type))))

(defn- index-report [{:keys [tx-data]}]
  (let [types (set (search/indexable-types))
        entities (->> tx-data
                      (map #(.e %))
                      (set)
                      (map (fn [eid]
                             (when-let [entity (entity/find eid)]
                               (when (contains? types (:schema/type entity))
                                 entity))))
                      (remove nil?))]
    (doseq [entity entities]
      (index-entity entity))))

(tx-processor/add-callback! ::indexer index-report)
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
   [taoensso.timbre :as timbre]
   [clojure.set :as set]))

(defn- ref->es [{:schema/keys [type] :as entity}]
  (case type
    :schema.type/i18n
    (->> (entity/serialize entity)
         (utils/mapm (fn [[culture value]]
                       [(->> culture entity/find :i18n.culture/keyword)
                        value])))
    :schema.type/amount
    (:amount/value entity)

    (:db/id entity)))

(defn- value->es [a v]
  (cond
    (= a :product/categories)
    (set (mapcat entities.category/get-parents v))

    (number? v)
    (if-let [entity (entity/find v)]
      (ref->es entity)
      v)

    :default v))

(defn- filter-entity-attr [e [a v]]
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
                 (map (fn [[a v]]
                        [a (value->es a v)]))
                 (reduce filter-entity-attr
                         {})
                 (common.utils/map-keys search.schema/ident->property)
                 (db-id->document-id))]
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
    (timbre/info "Starting tx-report-queue loop")
    (start-tx-report-queue-loop!))
  :stop
  (do
    (timbre/info "Stopping tx-report-queue loop")
    (future-cancel tx-report-queue-loop)))
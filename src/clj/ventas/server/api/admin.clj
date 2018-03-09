(ns ventas.server.api.admin
  (:require
   [clojure.core.async :as core.async :refer [chan >! <! go-loop go]]
   [clojure.core.async.impl.protocols :as core.async.protocols]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.server.pagination :as pagination]
   [ventas.search :as search]
   [ventas.stats :as stats]
   [kinsky.client :as kafka]
   [taoensso.timbre :as timbre]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.entities.user :as entities.user]))

(defn- admin-check! [session]
  (let [{:user/keys [roles]} (api/get-user session)]
    (when-not (contains? roles :user.role/administrator)
      (throw (Exception. "This API request requires administration privileges")))))

(defn- register-admin-endpoint!
  ([kw f]
   (register-admin-endpoint! kw {} f))
  ([kw opts f]
   (api/register-endpoint!
    kw
    opts
    (fn [request {:keys [session] :as state}]
      (admin-check! session)
      (f request state)))))

(register-admin-endpoint!
 :admin.brands.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :brand))))

(register-admin-endpoint!
 :admin.entities.remove
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} state]
   (entity/delete id)))

(register-admin-endpoint!
 :admin.currencies.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :currency))))

(register-admin-endpoint!
 :admin.taxes.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :tax))))

(register-admin-endpoint!
 :admin.taxes.save
 {:spec ::entity/entity}
 (fn [{tax :params} state]
   (entity/upsert :tax (-> tax
                           (update :amount float)))))

(register-admin-endpoint!
 :admin.users.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (->> (entity/query :user)
        (filter (fn [{:user/keys [status]}]
                  (not= status :user.status/unregistered)))
        (map #(merge (entity/to-json % {:culture (api/get-culture session)})
                     (entity/dates (:db/id %)))))))

(register-admin-endpoint!
 :admin.users.addresses.list
 {:spec {:user ::entity/entity}}
 (fn [{:keys [user]} {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :address {:user user}))))

(register-admin-endpoint!
 :admin.users.save
 {:spec ::entity/entity}
 (fn [{user :params} state]
   (entity/upsert :user user)))

(register-admin-endpoint!
 :admin.image-sizes.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :image-size))))

(register-admin-endpoint!
 :admin.image-sizes.entities.list
 (fn [_ _]
   entities.image-size/entities))

(register-admin-endpoint!
 :admin.entities.find
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} _]
   (entity/find id)))

(register-admin-endpoint!
 :admin.entities.find-json
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} _]
   (entity/find-json id)))

(register-admin-endpoint!
 :admin.entities.pull
 {:spec {:id ::api/id}}
 (fn [{{:keys [id expr]} :params} _]
   (db/pull (or expr '[*]) id)))

(register-admin-endpoint!
 :admin.events.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ _]
   (->> (db/transaction-log)
        (take-last 10)
        (db/explain-txs)
        (filter :entity-id))))

(register-admin-endpoint!
 :admin.orders.get
 {:spec {:id ::api/id}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [{:order/keys [user] :as order} (entity/find id)]
     {:order (db/pull '[*] id)
      :user (when user
              (let [name (entities.user/get-name user)]
                {:text name
                 :value user}))
      :lines (map #(entity/find-json % {:culture (api/get-culture session)})
                  (-> order :order/lines))})))

(register-admin-endpoint!
 :admin.search
 {:spec {:search string?
         :attrs #{keyword?}}
  :doc "Does a fulltext search for `search` in the given `attrs`"}
 (fn [{{:keys [search attrs]} :params} {:keys [session]}]
   (let [culture (api/get-culture session)
         {culture-kw :i18n.culture/keyword} (entity/find culture)]
     (search/entities search attrs culture-kw))))


(register-admin-endpoint!
 :admin.orders.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :order))))

(register-admin-endpoint!
 :admin.orders.save
 {:spec ::entity/entity}
 (fn [{entity :params} _]
   (entity/upsert* entity)))

(register-admin-endpoint!
 :admin.products.save
 {:spec ::entity/entity}
 (fn [{product :params} _]
   (entity/upsert* (-> product
                       (common.utils/update-in-when-some
                        [:product/price :amount/value]
                        bigdec)))))

(register-admin-endpoint!
 :admin.discounts.save
 {:spec ::entity/entity}
 (fn [{discount :params} _]
   (entity/upsert* (-> discount
                       (common.utils/update-in-when-some
                        [:discount/amount :amount/value]
                        bigdec)))))

(register-admin-endpoint!
 :admin.product.terms.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :product.term))))

(register-admin-endpoint!
 :admin.discounts.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :discount))))

(register-admin-endpoint!
 :admin.datadmin.datoms
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ _]
   (->> (db/datoms :eavt)
        (map db/datom->map))))

(register-admin-endpoint!
 :admin.plugins.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ _]
   (->> (plugin/all)
        (map plugin/plugin)
        (map (fn [plugin]
               (select-keys plugin #{:name}))))))

(defn time-series [topics {:keys [min max interval]}]
  (search/search {:query {:bool {:must [{:terms {:topic topics}}
                                        {:range {:timestamp {:gte min
                                                             :lte max}}}]}}
                  :aggs {:events {:histogram {:field "timestamp"
                                              :interval interval}}}}))

(defn make-interval [min max n]
  (/ (- max min) (* n 1.0)))

(defn- check-kafka! []
  (when-not (stats/enabled?)
    (throw (Exception. "Kafka is disabled. Statistics won't work. Check :kafka :host in the configuration."))))

(register-admin-endpoint!
 :admin.configuration.get
 (fn [{ids :params} _]
   (entities.configuration/get ids)))

(register-admin-endpoint!
 :admin.configuration.set
 (fn [{config :params} _]
   (doseq [[k v] config]
     (entities.configuration/set k v))))

(register-admin-endpoint!
 :admin.stats.realtime
 (fn [{{:keys [topics min max]} :params} {:keys [channel]}]
   (check-kafka!)
   (let [ch (chan)
         realtime? (not max)
         max (or max (System/currentTimeMillis))
         interval (make-interval min max 100.0)]
     (go
       (if (<= max min)
         (>! ch [])
         (do
           (>! ch (-> (time-series topics {:min min
                                           :max max
                                           :interval interval})
                      (get-in [:body :aggregations :events :buckets])))
           (when realtime?
             (let [consumer (stats/start-consumer!)]
               (kafka/subscribe! consumer topics)
               (loop []
                 (when (and (not (core.async.protocols/closed? ch))
                            (not (core.async.protocols/closed? channel)))
                   (<! (core.async/timeout interval))
                   (let [records (->> (stats/poll consumer 50)
                                      :by-topic
                                      vals
                                      (apply concat))]
                     (>! ch [{:doc_count (count records)
                              :key (System/currentTimeMillis)}])
                     (recur)))))))))
     ch)))

(register-admin-endpoint!
 :admin.stats.stream
 {:doc "Directly streams a topic from Kafka. Probably not a very good idea."}
 (fn [{{:keys [topic]} :params} {:keys [channel]}]
   (check-kafka!)
   (let [ch (chan)]
     (go
      (let [consumer (stats/start-consumer!)]
        (kafka/subscribe! consumer [topic])
        (loop []
          (when (and (not (core.async.protocols/closed? ch))
                     (not (core.async.protocols/closed? channel)))
            (let [records (->> (stats/poll consumer 1000)
                               :by-topic
                               vals
                               (apply concat))]
              (when (seq records)
                (doseq [record records]
                  (>! ch record))))
            (recur)))))
     ch)))
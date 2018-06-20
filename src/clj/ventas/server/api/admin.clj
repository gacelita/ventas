(ns ventas.server.api.admin
  (:require
   [clojure.core.async :as core.async :refer [<! <!! >! chan go go-loop]]
   [clojure.core.async.impl.protocols :as core.async.protocols]
   [slingshot.slingshot :refer [throw+]]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.entities.order :as entities.order]
   [ventas.plugin :as plugin]
   [ventas.search :as search]
   [ventas.server.api :as api]
   [ventas.search.entities :as search.entities]
   [ventas.server.pagination :as pagination]
   [ventas.utils :as utils]
   [ventas.kafka :as kafka]
   [taoensso.timbre :as timbre]))

(defn- admin-check! [session]
  (let [{:user/keys [roles]} (api/get-user session)]
    (when-not (contains? roles :user.role/administrator)
      (throw+ {:type ::unauthorized}))))

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
 :admin.entities.find-serialize
 {:spec {:id ::api/ref}
  :doc "Returns a serialized entity. Should be used for read-only access to
        entity data."}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (api/find-serialize-with-session session id)))

(register-admin-endpoint!
 :admin.entities.pull
 {:spec {:id ::api/ref}
  :doc "Returns an entity by using the pull API.
        This is the preferred way of getting entities in the administration."}
 (fn [{{:keys [id expr]} :params} _]
   (db/pull (or expr '[*]) id)))

(register-admin-endpoint!
 :admin.entities.save
 {:spec ::entity/entity
  :doc "Saves an entity, updating it if it already exists.
        This is the preferred way of saving entities in the administration."}
 (fn [{entity :params} _]
   (entity/upsert* entity)))

(register-admin-endpoint!
 :admin.entities.remove
 {:spec {:id ::api/ref}
  :doc "Removes the given entity."}
 (fn [{{:keys [id]} :params} _]
   (entity/delete id)))

(register-admin-endpoint!
 :admin.entities.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :spec {:type keyword?}
  :doc "Returns a coll of serialized entities with the given type."}
 (fn [{{:keys [type filters]} :params} {:keys [session]}]
   (->> (entity/query type filters)
        (map (partial api/serialize-with-session session)))))

(register-admin-endpoint!
 :admin.users.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (->> (entity/query :user)
        (filter (fn [{:user/keys [status]}]
                  (not= status :user.status/unregistered)))
        (map #(merge (api/serialize-with-session session %)
                     (entity/dates (:db/id %)))))))

(register-admin-endpoint!
 :admin.image-sizes.entities.list
 (fn [_ _]
   (->> entities.image-size/entities
        (map (comp utils/dequalify-keywords db/touch-eid)))))

(register-admin-endpoint!
 :admin.events.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :doc "Returns a coll of Datomic transactions. Meant for the activity log section."}
 (fn [_ _]
   (->> (db/transaction-log)
        (take-last 10)
        (db/explain-txs)
        (filter :entity-id))))

(register-admin-endpoint!
 :admin.orders.get
 {:spec {:id ::api/ref}
  :doc "Like doing admin.entities.pull on an order, but also returns the lines
        of the order, serialized."}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [{:order/keys [lines]} (entity/find id)]
     {:order (db/pull '[*] id)
      :status-history (entities.order/status-history id)
      :lines (map (partial api/find-serialize-with-session session)
                  lines)})))

(register-admin-endpoint!
 :admin.search
 {:spec {:search string?
         :attrs #{keyword?}}
  :doc "Does a fulltext search for `search` in the given `attrs`"}
 (fn [{{:keys [search attrs]} :params} {:keys [session]}]
   (let [culture (api/get-culture session)]
     (search.entities/search search attrs culture))))

(register-admin-endpoint!
 :admin.datadmin.datoms
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :doc "Returns Datomic datoms. Currently abandoned."}
 (fn [_ _]
   (->> (db/datoms :eavt)
        (map db/datom->map))))

(register-admin-endpoint!
 :admin.plugins.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]
  :doc "Returns the list of registered plugins."}
 (fn [_ {:keys [session]}]
   (->> (plugin/all)
        (map (fn [[id plugin]]
               (-> plugin
                   (select-keys #{:name :type})
                   (update :name #(if (string? %)
                                    %
                                    (api/serialize-with-session session %)))
                   (assoc :id id)))))))

(register-admin-endpoint!
 :admin.configuration.set
 {:doc "Sets the given configuration key to the given value."}
 (fn [{config :params} {:keys [site]}]
   (doseq [[k v] config]
     (entities.configuration/set! k v site))))

(defn time-series [topics {:keys [min max interval]}]
  (search/search {:query {:bool {:must [{:terms {:topic topics}}
                                        {:range {:timestamp {:gte min
                                                             :lt max}}}]}}
                  :aggs (->> topics
                             (utils/mapm (fn [topic]
                                           [(keyword topic)
                                            {:filter {:term {:topic topic}}
                                             :aggs {:events {:date_histogram {:field "timestamp"
                                                                              :min_doc_count 0
                                                                              :extended_bounds {:min min
                                                                                                :max (dec max)}
                                                                              :interval interval}}}}])))}))

(defn- check-kafka! []
  (when-not (kafka/enabled?)
    (throw+ {:type ::kafka-disabled
             :message "Kafka is disabled. Statistics won't work. Check :kafka :host in the configuration."})))

(defn- extract-bucket-data [response topic]
  (let [data (get-in response
                     [:body :aggregations (keyword topic) :events :buckets])]
    (zipmap (map :key data) (map :doc_count data))))

(defn- get-last-key [response]
  (->> response
       (vals)
       (first)
       (sort-by first)
       (last)
       (key)))

(register-admin-endpoint!
 :admin.stats.realtime
 {:doc "Returns statistics for the given topics.
        `min` and `max` should be millisecond timestamps.
        `interval` can be:
           - an amount in milliseconds
           - a valid ES interval (see date_histogram docs)
        Continuous updates will be sent if `max` is nil and `interval` is a millisecond amount."}
 (fn [{{:keys [topics min max interval]} :params} {:keys [channel]}]
   {:pre [min interval (or (not max) (< min max))]}
   (check-kafka!)
   (let [realtime? (and (not max) (number? interval))
         max (or max (System/currentTimeMillis))
         make-request (fn [{:keys [min max]} & {:keys [realtime?]}]
                        (let [params {:min min
                                      :max max
                                      :interval interval}
                              response (time-series topics params)]
                          (utils/mapm
                           (fn [topic]
                             [topic (extract-bucket-data response topic)])
                           topics)))]
     (if-not realtime?
       (make-request {:min min :max max})
       (let [response-ch (chan)
             response (make-request {:min min :max max} :realtime? true)]
         (go
           (>! response-ch response)
           (<! (core.async/timeout 1000))
           (loop [last-key (get-last-key response)]
             (<! (core.async/timeout interval))
             (when (and (not (core.async.protocols/closed? response-ch))
                        (not (core.async.protocols/closed? channel)))
               (let [new-key (+ last-key interval)]
                 (>! response-ch
                     (make-request {:min new-key
                                    :max (+ new-key interval)} :realtime? true))
                 (recur new-key)))))
         response-ch)))))
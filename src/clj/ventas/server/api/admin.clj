(ns ventas.server.api.admin
  (:require
   [clojure.core.async :as core.async :refer [chan >! <! go-loop go]]
   [clojure.core.async.impl.protocols :as core.async.protocols]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.server.pagination :as pagination]
   [ventas.search :as search]
   [ventas.stats :as stats]
   [kinsky.client :as kafka]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.utils :as utils]
   [slingshot.slingshot :refer [throw+]]))

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
      :lines (map (partial api/find-serialize-with-session session)
                  lines)})))

(register-admin-endpoint!
 :admin.search
 {:spec {:search string?
         :attrs #{keyword?}}
  :doc "Does a fulltext search for `search` in the given `attrs`"}
 (fn [{{:keys [search attrs]} :params} {:keys [session]}]
   (let [culture (api/get-culture session)]
     (search/entities search attrs culture))))

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
 (fn [_ _]
   (->> (plugin/all)
        (map (fn [[id plugin]]
               (-> plugin
                   (select-keys #{:name :type})
                   (assoc :id id)))))))

(register-admin-endpoint!
 :admin.configuration.set
 {:doc "Sets the given configuration key to the given value."}
 (fn [{config :params} _]
   (doseq [[k v] config]
     (entities.configuration/set! k v))))

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
    (throw+ {:type ::kafka-disabled
             :message "Kafka is disabled. Statistics won't work. Check :kafka :host in the configuration."})))

(register-admin-endpoint!
 :admin.stats.realtime
 {:doc "Returns Kafka messages from the given topics.
        `min` and `max` should be millisecond timestamps."}
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
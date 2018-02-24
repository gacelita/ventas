(ns ventas.stats
  "Sends stats to Kafka"
  (:require
   [kinsky.client :as kafka]
   [mount.core :refer [defstate]]
   [clojure.core.async :as core.async :refer [go]]
   [taoensso.timbre :as timbre]
   [ventas.search :as search]
   [ventas.config :as config])
  (:import [org.apache.kafka.clients.consumer ConsumerRecords ConsumerRecord KafkaConsumer]
           [org.apache.kafka.common TopicPartition]
           [org.apache.kafka.common.errors InterruptException]))

(def topics ["http" "navigation" "search"])

(defn- kafka->es [{:keys [offset topic] :as record}]
  (-> record
      (assoc "db/id" (str topic "-" offset))))

(defn cr->data
  "Yield a clojure representation of a consumer record.
   Taken from kinsky (done for adding the timestamp field)"
  [^ConsumerRecord cr]
  {:key (.key cr)
   :offset (.offset cr)
   :partition (.partition cr)
   :topic (.topic cr)
   :value (.value cr)
   :timestamp (.timestamp cr)})

(defn consumer-records->data
  "Yield the clojure representation of topic.
   Taken from kinsky (to override cr->data)"
  [^ConsumerRecords crs]
  (let [->d  (fn [^TopicPartition p] [(.topic p) (.partition p)])
        ps   (.partitions crs)
        ts   (set (for [^TopicPartition p ps] (.topic p)))
        by-p (into {} (for [^TopicPartition p ps] [(->d p) (mapv cr->data (.records crs p))]))
        by-t (into {} (for [^String t ts] [t (mapv cr->data (.records crs t))]))]
    {:partitions   (vec (for [^TopicPartition p ps] [(.topic p) (.partition p)]))
     :topics       ts
     :count        (.count crs)
     :by-topic     by-t
     :by-partition by-p}))

(defn poll [^KafkaConsumer consumer timeout]
  (consumer-records->data (.poll @consumer timeout)))

(defn enabled? []
  (boolean (config/get :kafka :host)))

(defn kafka-url []
  (str (config/get :kafka :host)
       ":"
       (config/get :kafka :port)))

(defn start-consumer! [& {:keys [group]}]
  (kafka/consumer {:bootstrap.servers (kafka-url)
                   :group.id (or group (str (gensym "group")))}
                  (kafka/keyword-deserializer)
                  (kafka/edn-deserializer)))

(defn start-indexer! []
  (future
   (let [consumer (start-consumer!)]
     (kafka/subscribe! consumer topics)
     (loop []
       (when-not (Thread/interrupted)
         (try
           (let [records (->> (poll consumer 100)
                              :by-topic
                              vals
                              (apply concat))]
             (when (seq records)
               (doseq [record records]
                 (timbre/debug :kafka-indexer record)
                 (-> record
                     (kafka->es)
                     (search/document->indexing-queue)))))
           (catch InterruptedException _
             (.interrupt (Thread/currentThread)))
           (catch InterruptException _
             (.interrupt (Thread/currentThread)))
           (catch Throwable e
             (timbre/error (class e) (.getMessage e))))
         (recur))))))

(defn start-producer! []
  (kafka/producer {:bootstrap.servers (kafka-url)}
                  (kafka/keyword-serializer)
                  (kafka/edn-serializer)))

(defstate producer
  :start
  (when (enabled?)
    (timbre/info "Starting Kafka producer")
    (start-producer!))
  :stop
  (when (enabled?)
    (timbre/info "Stopping Kafka producer")
    (kafka/close! producer)))

(defn send! [topic value]
  (when (enabled?)
    (timbre/debug :kafka-producer {:topic topic
                                   :value value})
    (kafka/send! producer {:topic topic
                           :value value})))

(defstate kafka-indexer
  :start
  (when (enabled?)
    (timbre/info "Starting Kafka indexer")
    (start-indexer!))
  :stop
  (when (enabled?)
    (timbre/info "Stopping Kafka indexer")
    (future-cancel kafka-indexer)))

(defn record-http-event!
  "HTTP traffic stats"
  [req]
  (send! "http"
         {:uri (get-in req [:uri])
          :host (get-in req [:headers :host])
          :language (get-in req [:headers :accept-language])
          :user-agent (get-in req [:headers :user-agent])}))

(defn record-navigation-event!
  "CLJS navigation stats"
  [{:keys [user handler params]}]
  (send! "navigation"
         {:user user
          :handler handler
          :params params}))

(defn record-search-event!
  "Search stats"
  [text]
  (send! "search"
         {:text text}))

(defn wrap-stats
  "Ring middleware for recording HTTP events"
  [handler]
  (fn [req]
    (when (future? kafka-indexer)
      (record-http-event! req))
    (handler req)))


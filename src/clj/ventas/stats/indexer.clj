(ns ventas.stats.indexer
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [ventas.kafka :as kafka]
   [ventas.kafka.consumer :as kafka.consumer]
   [ventas.search :as search]
   [ventas.kafka.registry :as kafka.registry]))

(defn- kafka->es [{:keys [offset topic] :as record}]
  (-> record
      (assoc "document/id" (str topic "-" offset))))

(def topics ["http" "navigation" "search"])

(defn start-indexer! []
  (kafka.registry/add!
   ::indexer
   (kafka.consumer/consume!
    {:topics topics
     :min-batch-size 0
     :consumer-group ::indexer}
    (fn [records]
      (doseq [record records]
        (timbre/debug :kafka-indexer record)
        (-> record
            (kafka->es)
            (search/document->indexing-queue)))))))

(defstate indexer
  :start
  (when (kafka/enabled?)
    (timbre/info "Starting Kafka indexer")
    (start-indexer!))
  :stop
  (when (kafka/enabled?)
    (timbre/info "Stopping Kafka indexer")
    (kafka.registry/remove! ::indexer)))

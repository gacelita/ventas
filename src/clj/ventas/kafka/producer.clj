(ns ventas.kafka.producer
  (:require
   [ventas.kafka :as kafka]
   [mount.core :refer [defstate]]
   [kinsky.client :as kinsky]
   [taoensso.timbre :as timbre]))

(defn start-producer! []
  (kinsky/producer {:bootstrap.servers (kafka/url)}
                   (kinsky/keyword-serializer)
                   (kinsky/edn-serializer)))

(defstate producer
  :start
  (when (kafka/enabled?)
    (timbre/info "Starting Kafka producer")
    (start-producer!))
  :stop
  (when (kafka/enabled?)
    (timbre/info "Stopping Kafka producer")
    (kinsky/close! producer)))

(defn send! [topic value]
  (when (kafka/enabled?)
    (timbre/debug :kafka-producer {:topic topic
                                   :value value})
    (kinsky/send! producer {:topic topic
                            :value value})))
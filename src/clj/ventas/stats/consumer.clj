(ns ventas.stats.consumer
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [kinsky.client :as kafka]
   [ventas.config :as config]))

(defn kafka-url []
  (str (config/get :kafka :host)
       ":"
       (config/get :kafka :port)))

(defn start-consumer! [& {:keys [group]}]
  (kafka/consumer {:bootstrap.servers (kafka-url)
                   :group.id (or group (str (gensym "group")))}
                  (kafka/keyword-deserializer)
                  (kafka/edn-deserializer)))

(def topics ["http" "navigation" "search"])

(defstate consumer
  :start
  (do
    (timbre/info "Starting consumer")
    (doto (start-consumer!)
      (kafka/subscribe! topics)))
  :stop
  (do
    (timbre/info "Stopping consumer")
    (kafka/close! consumer)))
(ns ventas.stats
  "Sends stats to Kafka"
  (:require
   [ventas.kafka.producer :as kafka.producer]
   [ventas.stats.indexer :as indexer]))

(defn record-http-event!
  "HTTP traffic stats"
  [req]
  (kafka.producer/send! "http"
                        {:uri (get-in req [:uri])
                         :host (get-in req [:headers :host])
                         :language (get-in req [:headers :accept-language])
                         :user-agent (get-in req [:headers :user-agent])}))

(defn record-navigation-event!
  "CLJS navigation stats"
  [{:keys [user handler params]}]
  (kafka.producer/send! "navigation"
                        {:user user
                         :handler handler
                         :params params}))

(defn record-search-event!
  "Search stats"
  [text]
  (kafka.producer/send! "search"
                        {:text text}))

(defn wrap-stats
  "Ring middleware for recording HTTP events"
  [handler]
  (fn [req]
    (when (future? indexer/indexer)
      (record-http-event! req))
    (handler req)))

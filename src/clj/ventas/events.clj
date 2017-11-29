(ns ventas.events
  (:require
   [clojure.core.async :as core.async :refer [<! >! go-loop chan]]
   [taoensso.timbre :refer [debug]]))

(defonce events (atom {}))

(defn register-event! [evt-name]
  (let [ch (chan)
        evt-data {:chan ch :mult (core.async/mult ch)}]
    (swap! events assoc evt-name evt-data)
    evt-data))

(defn event [evt-name]
  (if-let [evt-data (get @events evt-name)]
    evt-data
    (register-event! evt-name)))

(defn pub [evt-name]
  (let [data (event evt-name)]
    (get data :chan)))

(defn sub [evt-name]
  (let [data (event evt-name)]
    (core.async/tap (get data :mult) (chan))))
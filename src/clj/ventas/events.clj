(ns ventas.events
  (:require
   [clojure.core.async :as core.async :refer [<! >! go-loop chan]]
   [taoensso.timbre :refer [debug]]))

(defonce events (atom {}))

(defn event [evt-name]
  (let [evt-data (get @events evt-name)]
    (when-not evt-data
      (throw (Exception. (str "No event named " evt-name))))
    evt-data))

(defn pub [evt-name]
  (let [data (event evt-name)]
    (get data :chan)))

(defn sub [evt-name]
  (let [data (event evt-name)]
    (core.async/tap (get data :mult) (chan))))

(defn register-event! [evt-name]
  (let [ch (chan)
        evt-data {:chan ch :mult (core.async/mult ch)}]
    (swap! events assoc evt-name evt-data)
    (go-loop []
      (when (<! (sub evt-name))
        (debug "Event happened: " evt-name)
        (recur)))))

(register-event! :db-init)
(register-event! :init)
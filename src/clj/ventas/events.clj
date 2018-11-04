(ns ventas.events
  (:require
   [clojure.core.async :as core.async :refer [<! >! chan go-loop sliding-buffer]]))

(defonce events (atom {}))

(defn register-event! [evt-name]
  (let [ch (chan (sliding-buffer 10))
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
  (let [data (event evt-name)
        ch (chan)]
    (core.async/tap (get data :mult) ch)
    ch))

;; cljs
(defmacro ns-subscribe [path]
  (let [caller-ns (str (:name (:ns &env)))]
    `(deref (re-frame.core/subscribe [:db (into [(keyword ~caller-ns "state")] ~path)]))))

(defmacro ns-dispatch [path value]
  (let [caller-ns (str (:name (:ns &env)))]
    `(re-frame.core/dispatch [:db (into [(keyword ~caller-ns "state")] ~path) ~value])))
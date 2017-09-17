(ns ventas.utils.ui
  (:require [re-frame.core :as rf]))

(defn with-handler [cb]
  (fn [e]
    (doto e
      .preventDefault
      .stopPropagation)
    (cb e)))

(defn wrap-with-model
  "Wraps a component with a model binding"
  [data]
  (-> data (assoc :default-value (get @(:model data) (keyword (:name data))))
      (assoc :on-change #(swap! (:model data) assoc (keyword (:name data)) (-> % .-target .-value)))))

(defn reg-kw-sub
  "Creates a keyword subscription if it does not exist."
  [kw]
  (when-not (rf/subscribe [kw])
    (rf/reg-sub kw (fn [db _] (-> db kw)))))
(ns ventas.utils.ui
  (:require [re-frame.core :as rf]
            [re-frame.registrar :as rf.registrar]))

(defn with-handler [cb]
  (fn [e]
    (doto e
      .preventDefault
      .stopPropagation)
    (cb e)))

(defn wrap-with-model
  "Adds a model binding to the props of a component"
  [data]
  (-> data
      (assoc :default-value (get @(:model data) (keyword (:name data))))
      (assoc :on-change #(swap! (:model data) assoc (keyword (:name data)) (-> % .-target .-value)))
      (dissoc :model)))
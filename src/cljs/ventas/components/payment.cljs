(ns ventas.components.payment
  "Entry point to payment methods"
  (:refer-clojure :exclude [methods])
  (:require [re-frame.core :as rf]))

(def state-key ::state)

(defonce ^:private methods (atom {}))

(defn get-methods []
  @methods)

(defn add-method [kw data]
  (swap! methods assoc kw data))

(rf/reg-sub
 ::errors
 (fn [db]
   (get-in db [state-key :errors])))

(rf/reg-event-db
 ::set-errors
 (fn [db [_ errors]]
   (assoc-in db [state-key :errors] errors)))
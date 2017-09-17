(ns ventas.api
  (:require
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [debug]]))

(rf/reg-event-db
 :ventas.api/success
 (fn [db [_ where what]]
   (debug :ventas.api/success where what)
   (assoc-in db where what)))

(rf/reg-event-fx
  :api/users.list
  (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.list} options)}))

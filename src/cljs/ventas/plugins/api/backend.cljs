(ns ventas.plugins.api.backend
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::describe
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :api.describe}
                       options)}))

(rf/reg-event-fx
 ::generate-params
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :api.generate-params}
                       options)}))
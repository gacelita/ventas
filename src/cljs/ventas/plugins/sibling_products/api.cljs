(ns ventas.plugins.sibling-products.api
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::sibling-products.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :ventas.plugins.sibling-products.core/list}
                       options)}))

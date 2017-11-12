(ns ventas.plugins.featured-products.api
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::featured-products.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :ventas.plugins.featured-products.core/featured-products.list}
                       options)}))

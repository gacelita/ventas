(ns ventas.plugins.featured-products.core
  (:require
   [re-frame.core :as rf]
   [ventas.components.product-list :as components.product-list]
   [ventas.events :as events]
   [ventas.plugins.featured-products.api :as backend]))

(rf/reg-event-fx
 ::featured-products.list
 (fn [_ _]
   {:dispatch [::backend/featured-products.list
               {:success [::events/db [::featured-products]]}]}))

(defn featured-products []
  (let [products @(rf/subscribe [::events/db [::featured-products]])]
    [components.product-list/product-list products]))

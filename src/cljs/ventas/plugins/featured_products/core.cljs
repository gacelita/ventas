(ns ventas.plugins.featured-products.core
  (:require
   [re-frame.core :as rf]
   [ventas.components.product-list :as components.product-list]
   [ventas.events :as events]
   [ventas.plugins.featured-products.api :as backend]))

(rf/reg-event-fx
 ::featured-products.list
 (fn [cofx [_]]
   {:dispatch [::backend/featured-products.list
               {:success #(rf/dispatch [::events/db [::featured-products] %])}]}))

(defn featured-products []
  (rf/dispatch [::featured-products.list])
  (fn []
    (let [products @(rf/subscribe [::events/db [::featured-products]])]
      [components.product-list/product-list products])))

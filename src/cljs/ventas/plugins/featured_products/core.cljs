(ns ventas.plugins.featured-products.core
  (:require
   [ventas.plugins.featured-products.api :as backend]
   [ventas.components.product-list :as components.product-list]
   [re-frame.core :as rf]
   [ventas.events :as events]))

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

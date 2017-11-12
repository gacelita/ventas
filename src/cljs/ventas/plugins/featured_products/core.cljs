(ns ventas.plugins.featured-products.core
  (:require
   [ventas.plugins.featured-products.api :as api]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::featured-products.list
 (fn [cofx [_]]
   {:dispatch [::api/featured-products.list
               {:success-fn #(rf/dispatch [:ventas/db [::featured-products] %])}]}))

(defn featured-products []
  (rf/dispatch [::featured-products.list])
  (fn []
    (let [products @(rf/subscribe [:ventas/db [::featured-products]])]
      (js/console.log "Products" products)
      [:ul
       [:li "Product A"]
       [:li "Product C"]])))

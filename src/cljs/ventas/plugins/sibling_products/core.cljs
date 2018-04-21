(ns ventas.plugins.sibling-products.core
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.plugins.sibling-products.api :as backend]
   [ventas.components.product-list :as components.product-list]))

(rf/reg-event-fx
 ::list
 (fn [_ [_ product-id]]
   {:dispatch [::backend/sibling-products.list
               {:params {:id product-id}
                :success [::events/db [::state product-id]]}]}))

(rf/reg-sub
 ::list
 (fn [db [_ product-id]]
   (get-in db [::state product-id])))

(defn sibling-products [product-id]
  (let [products @(rf/subscribe [::list product-id])]
    [components.product-list/product-list products]))

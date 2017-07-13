(ns ventas.components.cart
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [bidi.bidi :as bidi]
            [ventas.routes :refer [routes]]))

(rf/reg-sub
 ::cart
 (fn [db _] (-> db ::cart)))

(rf/reg-event-fx
 ::cart
 (fn [{:keys [db local-storage]} [_]]
   {:db (assoc db :cart (get local-storage :cart))}))

(rf/reg-event-fx
 ::add
 (fn [{:keys [db local-storage]} [_]]
   {}))

(rf/reg-sub
  :cart-count
  (fn [_]
    (rf/subscribe [:cart-items]))
  (fn [items]
    (count items)))

(defn sidebar []
  "Cart"
  (rf/dispatch [::cart])
  (fn []
    [:div.ventas.cart-sidebar
     [:pre (.stringify js/JSON (clj->js @(rf/subscribe [::cart])) nil 2)]]))
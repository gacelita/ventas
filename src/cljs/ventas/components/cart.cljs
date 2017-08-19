(ns ventas.components.cart
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [clojure.string :as s]
            [bidi.bidi :as bidi]
            [ventas.components.base :as base]
            [ventas.routes :refer [routes]]
            [ventas.utils.ui :refer [with-handler]]))

;; Main state subscription
(rf/reg-sub
 ::main
 (fn [db _] (-> db :cart)))

;; Count of card items
(rf/reg-sub
 ::item-count
 (fn [_]
   (rf/subscribe [::main]))
 (fn [state]
   (js/console.log "full state" state)))

;; Items
(rf/reg-sub
 ::items
 (fn [_]
   (rf/subscribe [::main]))
 (fn [state]
   (js/console.log "the state" state)
   (-> state :items)))

;; Put the cart state in the app-db
(rf/reg-event-fx
 ::cart
 (fn [{:keys [db local-storage]} [_]]
   (js/console.log "local-storage" local-storage)
   {:db (assoc db :cart (get local-storage :cart))}))

;; Add item to the cart
(rf/reg-event-fx
 ::add
 (fn [{:keys [db local-storage]} [_ item]]
   (js/console.log "add" "db" db)
   {:db (assoc-in db [:cart :items (:id item)] item)}))

;; Remove item from the cart
(rf/reg-event-fx
 ::remove
 (fn [{:keys [db local-storage]} [_ item-id]]
   (js/console.log "remove" "db" db)
   {:db (update-in db [:cart :items] #(dissoc % item-id))}))

(defn cart-item [item]
  [:div.cart__item
   [:p (:name item)]])

(defn cart
  "Cart main view"
  []
  (let [visible @(rf/subscribe [::sidebar-visible])]
    [:div.cart__sidebar {:class (when visible "cart__sidebar--visible")}
     [:div.cart__sidebar--items
      (let [items @(rf/subscribe [::items])]
        (if (seq items)
          (for [[id item] items]
            ^{:key id} [cart-item item])
          [:p.cart__sidebar-no-items "No items"]))]
     [:button "Checkout"]
     [:button "Cart"]]))

(defn hover-item [item]
  [:div.cart__hover-item
   [:p (:name item)]
   [base/icon {:name "remove" :on-click (with-handler #(rf/dispatch [::remove (:id item)]))}]])

(defn hover
  "Cart hover"
  [id {:keys [visible]}]
  [:div.cart__hover {:class (when visible "cart__hover--visible")}
   [:div.cart__hover-items
    (let [items @(rf/subscribe [::items])]
      (if (seq items)
        (for [[id item] items]
          ^{:key id} [hover-item item])
        [:p.cart__hover-no-items "No items"]))]
   [:button "Checkout"]
   [:button "Cart"]])

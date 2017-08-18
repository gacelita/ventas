(ns ventas.components.cart
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [bidi.bidi :as bidi]
            [ventas.routes :refer [routes]]))

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
 (fn [{:keys [db local-storage]} [_ item]]
   (js/console.log "remove" "db" db)
   {:db (update-in db [:cart :items] #(dissoc % (:id item)))}))

(defn sidebar
  "Cart sidebar"
  []
  (fn []
    [:div.cart__sidebar]))

(defn item-view [item]
  [:div.cart__hover-item
   [:p (:name item) [sa/Icon {:name "remove"}]]])

(defn hover
  "Cart hover"
  [id {:keys [visible]}]
  (js/console.log "visible? " visible)
  [:div.cart__hover {:class (when visible "cart__hover--visible")}
   (js/console.log "about to subscribe")
   (for [[id item] @(rf/subscribe [::items])]
     [item-view item])])

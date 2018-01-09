(ns ventas.components.cart
  (:require
   [ventas.utils :as util]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [clojure.string :as s]
   [ventas.components.base :as base]
   [ventas.utils.ui :refer [with-handler]]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.utils.formatting :as formatting]
   [ventas.i18n :refer [i18n]]))

(rf/reg-sub
 ::main
 (fn [db _] (-> db :cart)))

(rf/reg-sub
 ::item-count
 (fn [_]
   (rf/subscribe [::main]))
 (fn [state]))

(rf/reg-sub
 ::items
 (fn [_]
   (rf/subscribe [::main]))
 (fn [state]
   (-> state :items)))

(rf/reg-event-db
 ::cart
 (fn [db [_ cart]]
   (assoc db :cart cart)))

(rf/reg-event-fx
 ::get
 (fn [db [_]]
   {:dispatch [::backend/users.cart.get {:success ::cart}]}))

(rf/reg-event-fx
 ::add
 (fn [db [_ eid]]
   {:dispatch [::backend/users.cart.add {:success ::add.next
                                         :params {:id eid}}]}))

(defn- notification-view [{:keys [id quantity product-variation] :as line}]
  (let [{:keys [images name price variation]} product-variation]
    [:div
     [:h3 (i18n ::product-added)]
     [:div.cart-notification__inner
      [:div.cart-notification__image
       [:img {:src (str "/images/" (:id (first images)) "/resize/product-listing")}]]
      [:div.cart-notification__info
       [:h4 (:name product-variation)]
       (let [{:keys [value currency]} price]
         [:h4 (str (formatting/format-number value) " " (:symbol currency))])
       [:table
        [:tbody
         (for [{:keys [taxonomy selected]} variation]
           [:tr
            [:td (:name taxonomy)]
            [:td (:name selected)]])]]]]]))

(rf/reg-event-fx
 ::add.next
 (fn [db [_ cart]]
   {:dispatch-n [[::cart cart]
                 [::notificator/add {:theme "cart-notification"
                                     :component [notification-view (last (:lines cart))]}]]}))

(rf/reg-event-fx
 ::remove
 (fn [db [_ eid]]
   {:dispatch [::backend/users.cart.remove {:success ::cart
                                            :params {:id eid}}]}))

(rf/reg-event-fx
 ::set-quantity
 (fn [db [_ eid quantity]]
   {:dispatch [::backend/users.cart.set-quantity {:success ::cart
                                                  :params {:id eid
                                                           :quantity quantity}}]}))

;; Remove item from the cart
(rf/reg-event-fx
 ::remove
 (fn [{:keys [db local-storage]} [_ item-id]]
   {:db (update-in db [:cart :items] #(dissoc % item-id))}))

(defn cart-item [item]
  [:div.cart__item
   [:p (:name item)]])

(defn cart
  "Cart main view"
  []
  [:div.cart
   [:div.cart__items
    (let [items @(rf/subscribe [::items])]
      (if (seq items)
        (for [[id item] items]
          ^{:key id} [cart-item item])
        [:p.cart__items "No items"]))]
   [:button "Checkout"]
   [:button "Cart"]])

(defn hover-item [item]
  [:div.cart__hover-item
   [:p.hover-item__quantity (:quantity item)]
   [:p.hover-item__name (:name item)]
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
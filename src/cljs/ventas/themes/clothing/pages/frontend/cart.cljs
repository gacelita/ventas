(ns ventas.themes.clothing.pages.frontend.cart
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.components.image :as image]
   [ventas.components.term :as term]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as utils]
   [ventas.utils.formatting :as utils.formatting]
   [ventas.components.error :as error]))

(rf/reg-event-fx
 ::add-voucher
 (fn [cofx event]
   ;; @TODO
))

(rf/reg-event-fx
 ::checkout
 (fn [cofx event]
   {:go-to [:frontend.checkout]}))

(rf/reg-sub
 ::line-count
 (fn [_]
   (rf/subscribe [::cart/main]))
 (fn [{:keys [lines]}]
   (count lines)))

(rf/reg-sub
 ::subtotal
 (fn [_]
   (rf/subscribe [::cart/main]))
 (fn [{:keys [lines]}]
   (reduce +
           (map #(* (:quantity %)
                    (get-in % [:product-variation :price :value]))
                lines))))

(rf/reg-sub
 ::shipping
 (fn [_]
   (rf/subscribe [::cart/main]))
 (fn [cart]
   ;; @TODO
   0))

(rf/reg-sub
 ::total
 (fn [_]
   [(rf/subscribe [::subtotal])
    (rf/subscribe [::shipping])])
 (fn [[subtotal shipping]]
   (+ subtotal shipping)))

(defn cart-sidebar []
  (let [voucher (atom nil)]
    (fn []
      [:div.cart-page__sidebar
       [:div.cart-page__sidebar-inner
        [:div.cart-page__sidebar-heading (i18n ::total)]
        [:div.cart-page__sidebar-content
         [:div.cart-page__discount
          [:p (i18n ::add-voucher)]
          [base/form-input {:on-change (utils/value-handler
                                        #(reset! voucher %))}]
          [base/button {:type "button"
                        :on-click #(rf/dispatch [::add-voucher])}
           (i18n ::add)]]
         [:table.cart-page__totals
          [:tbody
           [:tr
            [:td (str (i18n ::subtotal))]
            [:td (let [subtotal @(rf/subscribe [::subtotal])]
                   (utils.formatting/format-number subtotal))]]
           [:tr
            [:td (str (i18n ::shipping))]
            [:td (let [amount @(rf/subscribe [::shipping])]
                   (if (pos? amount)
                     (utils.formatting/format-number amount)
                     (i18n ::free)))]]
           [:tr
            [:td (str (i18n ::total))]
            [:td (let [total @(rf/subscribe [::total])]
                   (utils.formatting/format-number total))]]]]

         [base/button {:type "button"
                       :class "cart-page__checkout"
                       :on-click #(rf/dispatch [::checkout])}
          (i18n ::checkout)]]]])))

(defn cart-line-view [{:keys [product-variation quantity]}]
  [:div.cart-page__line
   [:div.cart-page__line-thumbnail
    [image/image (-> product-variation :images first :id) :cart-page-line]]
   [:div.cart-page__line-content
    [:div.cart-page__name
     [:h4 (:name product-variation)]]
    [:div.cart-page__price
     [:h4 (utils.formatting/amount->str (:price product-variation))]]
    [:div.cart-page__price
     [:h4 (str (i18n ::total) ": "
               (let [{:keys [currency value]} (:price product-variation)]
                 (utils.formatting/amount->str
                  {:currency currency
                   :value (* quantity value)})))]]
    [:div.cart-page__terms
     (let [variation-data (get product-variation :variation)]
       (for [{:keys [taxonomy selected]} variation-data]
         (when selected
           [:div.cart-page__term
            [term/term-view (:keyword taxonomy) selected {:active? true}]])))]

    [:div.cart-page__actions
     [base/button {:icon true
                   :basic true
                   :color "red"
                   :on-click #(rf/dispatch [::cart/remove (:id product-variation)])}
      [base/icon {:name "trash"}]]
     [base/button {:icon true
                   :basic true
                   :color "red"
                   :on-click #(rf/dispatch [::events/users.favorites.toggle (:id product-variation)])}
      [base/icon {:name (if @(rf/subscribe [::events/users.favorites.favorited? (:id product-variation)])
                          "heart"
                          "empty heart")}]]
     [base/select {:default-value quantity
                   :on-change #(rf/dispatch [::cart/set-quantity (:id product-variation) (.-value %2)])
                   :options (clj->js (for [n (range 1 16)]
                                       {:value n :text (str n)}))}]]]])

(defn- no-items []
  [error/no-data :message (i18n ::no-items)])

(defn page []
  [skeleton
   [base/container
    (let [{:keys [lines] :as cart} @(rf/subscribe [::cart/main])]
      [:div.cart-page
       [:h2 (i18n ::cart)]
       (if (and cart (not (seq lines)))
         [no-items]
         [:div.cart-page__content
          [:div.cart-page__lines
           (for [line lines]
             [cart-line-view line])]
          [cart-sidebar]])])]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::cart/get]}))

(routes/define-route!
  :frontend.cart
  {:name ::page
   :url ["cart"]
   :component page
   :init-fx [::init]})

(ns ventas.themes.clothing.pages.frontend.cart
  (:require
   [re-frame.core :as rf]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as util]
   [ventas.components.cart :as cart]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events :as events]
   [ventas.themes.clothing.components.heading :as theme.heading]
   [ventas.events.backend :as backend]
   [ventas.utils :as utils]))

(rf/reg-event-fx
 ::add-voucher
 (fn [cofx event]
   ;; @TODO
   ))

(defn cart-sidebar []
  (let [voucher (atom nil)]
    (fn []
      [:div.cart-page__sidebar
       [:div.cart-page__sidebar-heading (i18n ::total)]
       [:div.cart-page__discount
        (i18n ::add-voucher)
        [base/form-input {:on-change (utils/value-handler
                                      #(reset! voucher %))}]
        [base/button {:type "button"
                      :on-click #(rf/dispatch [::add-voucher])}
         (i18n ::add)]]])))

(defn cart-line-view [{:keys [product-variation quantity]}]
  (js/console.log "variation" product-variation "quantity" quantity)
  [:div.cart-page__line
   [:p quantity]])

(defn page []
  (rf/dispatch [::cart/get])
  (fn []
    [skeleton
     [base/container
      [:div.cart-page
       [:h2 (i18n ::cart)]
       [:div.cart-page__content
        [:div.cart-page__lines
         (let [{:keys [lines]} @(rf/subscribe [::events/db :cart])]
           (for [line lines]
             [cart-line-view line]))]
        [cart-sidebar]]]]]))

(routes/define-route!
 :frontend.cart
 {:name ::page
  :url ["login"]
  :component page})
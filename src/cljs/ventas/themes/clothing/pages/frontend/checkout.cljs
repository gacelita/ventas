(ns ventas.themes.clothing.pages.frontend.checkout
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.components.form :as form]
   [ventas.components.payment :as payment]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.plugins.stripe.core]
   [ventas.plugins.wire-transfer.core]
   [ventas.routes :as routes]
   [ventas.utils.formatting :as utils.formatting]
   [ventas.session :as session]
   [ventas.themes.clothing.components.address :as theme.address]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]))

(def state-key ::state)

(rf/reg-event-db
 ::set-shipping-method
 (fn [db [_ v]]
   (assoc-in db [state-key :shipping-method] v)))

(defn page []
  [theme.skeleton/skeleton
   [base/container
    [:div.checkout-page
     [:h2 (i18n ::checkout)]
     [:div.checkout-page__content
      (when-not (session/valid-identity?)
        [:div

         [:div
          [base/header {:as "h3"
                        :attached "top"
                        :class "checkout-page__contact-info"}
           [:span (i18n ::contact-information)]
           [:div.checkout-page__login
            (i18n ::already-registered)
            " "
            [:a {:href (routes/path-for :frontend.login)}
             (i18n ::login)]]]
          [base/segment {:attached true}
           [form/form [state-key]
            [base/form
             [form/field {:db-path [state-key]
                          :label (i18n ::email)}]]]]]

         [base/divider {:hidden true}]

         [:div.checkout-page__address
          [base/header {:as "h3"
                        :attached "top"}
           (i18n ::shipping-address)]
          [base/segment {:attached true}
           [theme.address/address [state-key]]]]

         [:div.checkout-page__shipping-methods
          (let [selected @(rf/subscribe [::events/db [state-key :shipping-method]])
                methods @(rf/subscribe [::events/db [state-key :shipping-methods]])]
            (doall
             (for [{:keys [id name price]} methods]
               [:div.shipping-method
                [base/segment
                 [base/form-radio
                  {:value id
                   :checked (= id selected)
                   :on-change #(rf/dispatch [::set-shipping-method (aget %2 "value")])}]
                 [:span.shipping-method__name name]
                 [:span.shipping-method__price (utils.formatting/amount->str price)]]])))]

         [:div.checkout-page__payment-methods
          (doall
           (for [[_ {:keys [component]}] (payment/get-methods)]
             [component]))]])]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::cart/get]
                 [::backend/users.cart.shipping-methods
                  {:success [::events/db [state-key :shipping-methods]]}]
                 [::theme.address/init [state-key]]]}))

(routes/define-route!
  :frontend.checkout
  {:name ::page
   :url ["checkout"]
   :component page
   :init-fx [::init]})

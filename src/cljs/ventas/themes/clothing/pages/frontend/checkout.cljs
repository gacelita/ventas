(ns ventas.themes.clothing.pages.frontend.checkout
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
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
   [ventas.session :as session]
   [ventas.themes.clothing.components.address :as theme.address]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]
   [ventas.themes.clothing.pages.frontend.profile.addresses :as profile.addresses]
   [ventas.utils.formatting :as utils.formatting]))

(def state-key ::state)

(rf/reg-event-db
 ::set-shipping-method
 (fn [db [_ v]]
   (assoc-in db [state-key :shipping-method] v)))

(rf/reg-event-db
 ::set-payment-method
 (fn [db [_ v]]
   (assoc-in db [state-key :payment-method] v)))

(rf/reg-event-db
 ::set-shipping-address
 (fn [db [_ v]]
   (assoc-in db [state-key :shipping-address] v)))

(rf/reg-event-fx
 ::order
 (fn [{:keys [db]} _]
   (let [{:keys [shipping-method payment-method]} (get db state-key)
         payment-params (form/get-data db [state-key :payment-params])
         email (:email (form/get-data db [state-key :contact-information]))
         shipping-address (let [address (get-in db [state-key :shipping-address])]
                            (if-not (= -1 address)
                              address
                              (->> (form/get-data db [state-key :new-address])
                                   (common.utils/map-keys #(keyword (name %))))))]
     {:dispatch [::backend/users.cart.order
                 {:params {:payment-params payment-params
                           :email email
                           :shipping-address shipping-address
                           :shipping-method shipping-method
                           :payment-method payment-method}}]})))

(defn contact-information []
  [:div.checkout-page__contact-information
   [base/header {:as "h3"
                 :attached "top"
                 :class "checkout-page__contact-info"}
    [:span (i18n ::contact-information)]
    [:div.checkout-page__login
     (i18n ::already-registered)
     " "
     [:a {:href (routes/path-for :frontend.login)}
      (i18n ::login)]]]
   (let [db-path [state-key :contact-information]]
     [base/segment {:attached true}
      [form/form db-path
       [base/form
        [form/field {:db-path db-path
                     :label (i18n ::email)
                     :key :email}]]]])])

(defn address []
  [:div.checkout-page__address
   [base/header {:as "h3"
                 :attached "top"}
    (i18n ::shipping-address)]
   [base/segment {:attached true}
    (let [shipping-address @(rf/subscribe [::events/db [state-key :shipping-address]])]
      [:div.checkout-page__address-inner
       [base/form
        (doall
         (for [{:keys [id] :as address} @(rf/subscribe [::events/db [state-key :addresses]])]
           [base/segment
            [base/form-radio {:value id
                              :checked (= shipping-address id)
                              :on-change #(rf/dispatch [::set-shipping-address id])}]
            [profile.addresses/address-content-view address]]))
        [base/segment
         [base/form-radio {:value -1
                           :checked (= -1 shipping-address)
                           :on-change #(rf/dispatch [::set-shipping-address -1])}]
         [:span (i18n ::new-address)]]]
       (when (= shipping-address -1)
         [theme.address/address [state-key :new-address]])])]])

(defn shipping-methods []
  [:div.checkout-page__shipping-methods
   [base/header {:as "h3"
                 :attached "top"}
    (i18n ::shipping-method)]
   [base/segment {:attached true}
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
           [:span.shipping-method__price (utils.formatting/amount->str price)]]])))]])

(defn payment-methods []
  [:div.checkout-page__payment-methods
   [base/header {:as "h3"
                 :attached "top"}
    (i18n ::payment-method)]
   [base/segment {:attached true}
    (let [selected @(rf/subscribe [::events/db [state-key :payment-method]])
          methods @(rf/subscribe [::events/db [state-key :payment-methods]])
          components (payment/get-methods)]
      [base/accordion {:fluid true
                       :styled true}
       [:div.payment-methods
        (map-indexed
         (fn [idx [id {:keys [name]}]]
           [:div.payment-method
            [base/accordion-title {:active (= selected id)
                                   :index idx
                                   :on-click #(rf/dispatch [::set-payment-method id])}
             [:span name]]
            [base/accordion-content {:active (= selected id)}
             [:div
              (when-let [component (get-in components [id :component])]
                [component [state-key :payment-params]])]]])
         methods)]])]])

(defn page []
  [theme.skeleton/skeleton
   [base/container
    [:div.checkout-page
     [:h2 (i18n ::checkout)]
     [:div.checkout-page__content
      [:div
       (when-not (session/valid-identity?)
         [:div
          [contact-information]
          [base/divider {:hidden true}]])
       [address]
       [base/divider {:hidden true}]
       [shipping-methods]
       [base/divider {:hidden true}]
       [payment-methods]
       [base/divider {:hidden true}]
       [base/button {:type "button"
                     :size "large"
                     :fluid true
                     :on-click #(rf/dispatch [::order])}
        (i18n ::order)]]]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::cart/get]
                 [::backend/users.addresses
                  {:success [::events/db [state-key :addresses]]}]
                 [::backend/users.cart.shipping-methods
                  {:success ::init.shipping-methods.next}]
                 [::backend/users.cart.payment-methods
                  {:success ::init.payment-methods.next}]
                 [::theme.address/init [state-key]]]}))

(rf/reg-event-fx
 ::init.shipping-methods.next
 (fn [_ [_ methods]]
   {:dispatch-n [[::events/db [state-key :shipping-methods] methods]
                 [::set-shipping-method (-> methods first :id)]]}))

(rf/reg-event-fx
 ::init.payment-methods.next
 (fn [_ [_ methods]]
   {:dispatch-n (->> methods
                     (map (fn [[id _]]
                            (js/console.log :id id :what (-> (payment/get-methods) id))
                            (when-let [init-fx (-> (payment/get-methods) id :init-fx)]
                              init-fx)))
                     (into [[::events/db [state-key :payment-methods] methods]
                            [::set-payment-method (-> methods first key)]]))}))

(routes/define-route!
  :frontend.checkout
  {:name ::page
   :url ["checkout"]
   :component page
   :init-fx [::init]})

(ns ventas.plugins.stripe.frontend
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [ventas.components.payment :as payment]
   [react-stripe-elements]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.utils.ui :as utils.ui]
   [ventas.components.base :as base]))

(def stripe-elements (r/adapt-react-class (.-Elements react-stripe-elements)))
(def card-cvc-element (r/adapt-react-class (.-CardCVCElement react-stripe-elements)))
(def card-element (r/adapt-react-class (.-CardElement react-stripe-elements)))
(def card-expiry-element (r/adapt-react-class (.-CardExpiryElement react-stripe-elements)))
(def card-number-element (r/adapt-react-class (.-CardNumberElement react-stripe-elements)))
(def postal-code-element (r/adapt-react-class (.-PostalCodeElement react-stripe-elements)))
(def stripe-provider (r/adapt-react-class (.-StripeProvider react-stripe-elements)))

(def state-key ::state)

(rf/reg-event-fx
 ::set-error
 (fn [{:keys [db]} [_ field error]]
   (let [errors (get-in db [state-key :errors])
         errors (if error
                  (assoc errors field error)
                  (dissoc errors field))]
     {:db (assoc-in db [state-key :errors] errors)
      :dispatch [::payment/set-errors errors]})))

(defn- on-element-change [e]
  (let [e (js->clj e :keywordize-keys true)
        error (:error e)]
    (rf/dispatch [::set-error (keyword (:elementType e)) error])))

(rf/reg-sub
 ::errors
 (fn [db]
   (get-in db [state-key :errors])))

(rf/reg-sub
 ::errors?
 (fn [_]
   (rf/subscribe [::errors]))
 (fn [errors]
   (not (every? nil? (vals errors)))))

(rf/reg-sub
 ::error
 (fn [_]
   (rf/subscribe [::errors]))
 (fn [errors [_ key]]
   (get errors key)))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} [_ {:keys [success error]}]]
   (let [stripe-instance (get-in db [state-key :stripe-instance])]
     (.then (.createToken (aget stripe-instance "props" "stripe"))
            (fn [payload]
              (if-let [token (.-token payload)]
                (rf/dispatch (conj success {:token (.-id token)}))
                (rf/dispatch error))))
     {})))

(defn- stripe-form-render [this]
  [:form.stripe-form
   [:label (i18n ::card-number)]
   [card-number-element {:on-change on-element-change
                         :class "stripe-form__field"}]
   (when-let [error @(rf/subscribe [::error :cardNumber])]
     [:div.stripe-form__error (:message error)])

   [:label (i18n ::expiration-date)]
   [card-expiry-element {:on-change on-element-change}]
   (when-let [error @(rf/subscribe [::error :cardExpiry])]
     [:div.stripe-form__error (:message error)])

   [:label (i18n ::cvc)]
   [card-cvc-element {:on-change on-element-change}]
   (when-let [error @(rf/subscribe [::error :cardCvc])]
     [:div.stripe-form__error (:message error)])

   [:label (i18n ::postal-code)]
   [postal-code-element {:on-change on-element-change}]
   (when-let [error @(rf/subscribe [::error :postalCode])]
     [:div.stripe-form__error (:message error)])])

(rf/reg-event-db
 ::set-stripe-instance
 (fn [db [_ this]]
   (assoc-in db [state-key :stripe-instance] this)))

(def stripe-form
  (r/adapt-react-class
   (.injectStripe react-stripe-elements
    (r/reactify-component
     (r/create-class {:display-name "stripe-form"
                      :component-did-mount (fn [this]
                                             (rf/dispatch [::set-stripe-instance this]))
                      :render stripe-form-render})))))

(defn- include-stripe! []
  (.appendChild js/document.head
                (doto (js/document.createElement "script")
                  (aset "onload" (fn [_]
                                   (rf/dispatch [::events/db [state-key :loaded?] true])))
                  (aset "src" "https://js.stripe.com/v3"))))

(defn- main []
  (when-not @(rf/subscribe [::events/db [state-key :loaded?]])
    (include-stripe!))
  (fn []
    (let [public-key @(rf/subscribe [::events/db [:configuration :stripe.public-key]])]
      (when (and public-key @(rf/subscribe [::events/db [state-key :loaded?]]))
        [stripe-provider {:apiKey public-key}
         [stripe-elements [stripe-form]]]))))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::events/configuration.get #{:stripe.public-key}]}))

(payment/add-method
 :stripe
 {:component main
  :submit-fx [::submit]
  :init-fx [::init]})

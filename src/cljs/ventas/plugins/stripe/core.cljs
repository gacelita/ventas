(ns ventas.plugins.stripe.core
  "Stripe configuration"
  (:require
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [re-frame.core :as rf]
   [ventas.i18n :as i18n :refer [i18n]]
   [ventas.components.form :as form]
   [ventas.components.base :as base]
   [ventas.utils.ui :as utils.ui]
   [ventas.components.notificator :as notificator]
   [ventas.components.payment :as payment]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [reagent.core :as r])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(admin.skeleton/add-menu-item!
 :admin.payment-methods
 {:route :admin.payment-methods.stripe
  :label ::page})

(i18n/register-translations!
 {:en_US
  {::page "Stripe"
   ::stripe.publishable-key "Publishable key"
   ::stripe.private-key "Private key"
   ::submit "Submit"
   ::pay-with-stripe "Pay with Stripe"}})

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.configuration.set
               {:params (get-in db [state-key :form])
                :success [::notificator/notify-saved]}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(defn- content []
  [form/form [state-key]
   [base/segment {:color "orange"
                  :title (i18n ::page)}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     [field {:key :stripe.publishable-key}]
     [field {:key :stripe.private-key}]

     [base/divider {:hidden true}]

     [base/form-button
      {:type "submit"}
      (i18n ::submit)]]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-payment-methods-stripe__page
    [content]]])

(rf/reg-event-fx
 ::admin-init
 (fn [_ _]
   {:dispatch-n [[::backend/admin.configuration.get
                  {:params #{:stripe.publishable-key
                             :stripe.private-key}
                   :success [::form/populate [state-key]]}]]}))

(routes/define-route!
 :admin.payment-methods.stripe
 {:name ::page
  :url "stripe"
  :component page
  :init-fx [::admin-init]})


(defn- start-stripe! [form-node key]
  (let [node (js/document.createElement "script")]
    (.setAttribute node "data-key" key)
    (.appendChild form-node
                  (doto node
                    (aset "className" "stripe-button")
                    (aset "async" true)
                    (aset "src" "https://checkout.stripe.com/checkout.js")))))

(defn- stripe-mount-or-update [this _]
  (let [key @(rf/subscribe [::events/db [:configuration :stripe.publishable-key]])
        node (.querySelector (r/dom-node this) "form")]
    (when (and node key)
      (start-stripe! node key))))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::events/configuration.get #{:stripe.publishable-key}]}))

(defn stripe-checkout []
  (rf/dispatch [::init])
  (fn []
    (r/create-class
     {:component-did-mount stripe-mount-or-update
      :component-did-update stripe-mount-or-update
      :reagent-render
      (fn [props]
        @(rf/subscribe [::events/db [:configuration :stripe.publishable-key]])
        [:div
         [:h2 (i18n ::pay-with-stripe)]
         [:form {:action "localhost:3450"
                 :method "POST"}]])})))

(payment/add-method
 :stripe
 {:component stripe-checkout})

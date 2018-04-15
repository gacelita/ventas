(ns ventas.plugins.stripe.frontend
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [ventas.components.payment :as payment]
   [ventas.events :as events]))

(defn- start-stripe! [form-node key]
  (let [node (js/document.createElement "script")]
    (.setAttribute node "data-key" key)
    (.appendChild form-node
                  (doto node
                    (aset "className" "stripe-button")
                    (aset "async" true)
                    (aset "src" "https://checkout.stripe.com/checkout.js")))))

(defn- stripe-mount-or-update [this _]
  (let [key @(rf/subscribe [::events/db [:configuration :stripe.publickey]])
        node (.querySelector (r/dom-node this) "form")]
    (when (and node key)
      (start-stripe! node key))))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::events/configuration.get #{:stripe.public-key}]}))

(defn stripe-checkout []
  (r/create-class
   {:component-did-mount stripe-mount-or-update
    :component-did-update stripe-mount-or-update
    :reagent-render
    (fn [props]
      @(rf/subscribe [::events/db [:configuration :stripe.public-key]])
      [:div
       [:p "Not ready yet!"]
       [:form {:action "localhost:3450"
               :method "POST"}]])}))

(payment/add-method
 :stripe
 {:component stripe-checkout
  :init-fx [::init]})

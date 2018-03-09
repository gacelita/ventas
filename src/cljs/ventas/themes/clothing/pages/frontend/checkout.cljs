(ns ventas.themes.clothing.pages.frontend.checkout
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.plugins.stripe.core :as stripe]
   [ventas.events :as events]
   [ventas.components.payment :as payment]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]))

(def state-key ::state)

(defn page []
  [theme.skeleton/skeleton
   [base/container
    [:div.checkout-page
     [:h2 (i18n ::checkout)]
     [:div.checkout-page__content
      [:div.checkout-page__payment-methods
       (doall
        (for [{:keys [component]} (payment/get-methods)]
          [component]))]]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::cart/get]]}))

(routes/define-route!
  :frontend.checkout
  {:name ::page
   :url ["checkout"]
   :component page
   :init-fx [::init]})

(ns ventas.themes.clothing.pages.frontend.checkout
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]))

(defn page []
  [theme.skeleton/skeleton
   [base/container
    [:div.checkout-page
     [:h2 (i18n ::checkout)]
     [:div.checkout-page__content]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::cart/get]}))

(routes/define-route!
  :frontend.checkout
  {:name ::page
   :url ["checkout"]
   :component page
   :init-fx [::init]})

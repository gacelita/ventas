(ns ventas.themes.clothing.pages.frontend.checkout
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.components.cart :as cart]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

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

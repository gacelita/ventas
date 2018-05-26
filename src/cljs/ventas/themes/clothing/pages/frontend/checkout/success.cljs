(ns ventas.themes.clothing.pages.frontend.checkout.success
  (:require
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :as theme.skeleton]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

(defn page []
  [theme.skeleton/skeleton
   [base/container
    [:div.checkout-page
     [:h2 (i18n ::thank-you)]
     [:p (i18n ::order-placed)]]]])

(routes/define-route!
 :frontend.checkout.success
 {:name ::page
  :url ["success"]
  :component page})
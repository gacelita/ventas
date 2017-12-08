(ns ventas.themes.clothing.pages.frontend.privacy-policy
  (:require
   [ventas.page :refer [pages]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.components.base :as base]))

(defn content []
  [base/container
   [:h1 (i18n ::privacy-policy)]
   [:p (i18n ::privacy-policy-text)]])

(defn page []
  [skeleton
   [content]])

(routes/define-route!
 :frontend.privacy-policy
 {:name ::page
  :url ["privacy-policy"]
  :component page})
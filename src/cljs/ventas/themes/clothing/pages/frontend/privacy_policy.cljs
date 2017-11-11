(ns ventas.themes.clothing.pages.frontend.privacy-policy
  (:require
   [ventas.page :refer [pages]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]))

(defn page []
  [skeleton
   [:div
    [:p "Texto de ejemplo"]]])

(routes/define-route!
 :frontend.privacy-policy
 {:name (i18n ::page)
  :url ["privacy-policy"]
  :component page})
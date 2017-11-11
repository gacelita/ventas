(ns ventas.themes.clothing.pages.privacy-policy
  (:require [ventas.page :refer [pages]]
            [ventas.themes.clothing.components.skeleton :refer [skeleton]]))

(defmethod pages :frontend.privacy-policy []
  [skeleton
   [:div
    [:p "Texto de ejemplo"]]])

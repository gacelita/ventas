(ns ventas.themes.mariscosriasbajas.pages.privacy-policy
  (:require [ventas.page :refer [pages]]
            [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]))

(defmethod pages :frontend.privacy-policy []
  [skeleton
   [:div
    [:p "Texto de ejemplo"]]])

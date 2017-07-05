(ns ventas.components.menu
  (:require [fqcss.core :refer [wrap-reagent]]
            [soda-ash.core :as sa]
            [re-frame.core :as rf]))

(defn menu [items]
  (wrap-reagent
    [:div {:fqcss [::menu]}
      [:ul
       (for [item items]
         [:li
           [:a {:href (:href item)} (:text item)]])]]))
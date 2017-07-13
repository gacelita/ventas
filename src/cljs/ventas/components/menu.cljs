(ns ventas.components.menu
  (:require [fqcss.core :refer [wrap-reagent]]
            [soda-ash.core :as sa]
            [re-frame.core :as rf]))

(defn menu-item [data]
  [:li
   [:a {:href (:href data)} (:text data)]])

(defn menu [items]
  (wrap-reagent
    [:div {:fqcss [::menu]}
      [:ul
       (map-indexed
        (fn [item idx] ^{:key idx} [menu-item item])
         items)]]))
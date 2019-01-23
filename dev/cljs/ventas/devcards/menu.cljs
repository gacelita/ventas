(ns ventas.devcards.menu
  (:require
   [devcards.core :refer-macros [defcard-rg]]
   [ventas.components.menu :as components.menu]))

(defn component []
  [:div
   [components.menu/menu
    [{:href "cat" :text "A cat"}
     {:href "dog" :text "A doggo"}]]])

(defcard-rg regular-menu
  "Regular menu"
  component)

(ns ventas.devcards.menu
  (:require
   [devcards.core]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.components.menu :as components.menu])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn component []
  [:div
   [components.menu/menu
    [{:href "cat" :text "A cat"}
     {:href "dog" :text "A doggo"}]]])

(defcard-rg regular-menu
  "Regular menu"
  component)

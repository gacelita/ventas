(ns ventas.devcards.menu
  (:require
   [reagent.core :as reagent]
   [devcards.core])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn component []
  [:div [:h1 "This is your first devcard!"]])

(defcard-rg text-input-2
  component)
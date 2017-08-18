(ns ventas.devcards.core
  (:require
   [reagent.core :as reagent]
   [devcards.core]
   [ventas.devcards.menu]
   [ventas.devcards.breadcrumbs]
   [ventas.devcards.cart])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn component []
  [:div [:h1 "This is your first devcard!"]])

(defcard-rg text-input-1
 component)

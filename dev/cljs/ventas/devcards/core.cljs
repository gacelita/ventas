(ns ventas.devcards.core
  (:require
   [devcards.core :refer-macros [defcard-rg]]
   [ventas.devcards.breadcrumbs]
   [ventas.devcards.category-list]
   [ventas.devcards.menu]
   [ventas.devcards.cart]))

(defn component []
  [:div [:h1 "This is your first devcard!"]])
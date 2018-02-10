(ns ventas.devcards.core
  (:require
   [devcards.core]
   [ventas.devcards.breadcrumbs]
   [ventas.devcards.cart]
   [ventas.devcards.category-list]
   [ventas.devcards.menu]
   [ventas.devcards.product-list]
   [ventas.devcards.slider])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn component []
  [:div [:h1 "This is your first devcard!"]])

(defcard-rg text-input-1
  component)

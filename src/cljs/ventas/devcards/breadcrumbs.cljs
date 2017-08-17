(ns ventas.devcards.breadcrumbs
  (:require
   [reagent.core :as reagent]
   [devcards.core]
   [ventas.components.breadcrumbs :as components.breadcrumbs])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn component []
  [:div [:h1 "This is your first devcard!"]])

(defcard-rg regular-breadcrumb
  "Regular breadcrumb"
  [components.breadcrumbs/breadcrumb-view :frontend.index])

(defcard-rg parameterized-breadcrumb
  "Parameterized breadcrumb"
  [components.breadcrumbs/breadcrumb-view :frontend.product {:id 1}])
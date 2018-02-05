(ns ventas.devcards.breadcrumbs
  (:require
   [devcards.core]
   [reagent.core :as reagent]
   [ventas.components.breadcrumbs :as components.breadcrumbs])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defcard-rg regular-breadcrumb
  "Regular breadcrumb"
  [components.breadcrumbs/breadcrumb-view :frontend.privacy-policy])

(defcard-rg parameterized-breadcrumb
  "Parameterized breadcrumb"
  [components.breadcrumbs/breadcrumb-view :frontend.product {:id 1}])

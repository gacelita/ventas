(ns ventas.devcards.breadcrumbs
  (:require
   [devcards.core :refer-macros [defcard-rg]]
   [ventas.components.breadcrumbs :as components.breadcrumbs]))

(defcard-rg regular-breadcrumb
  "Regular breadcrumb"
  [components.breadcrumbs/breadcrumb-view :frontend.privacy-policy])

(defcard-rg parameterized-breadcrumb
  "Parameterized breadcrumb"
  [components.breadcrumbs/breadcrumb-view :frontend.product {:id 1}])

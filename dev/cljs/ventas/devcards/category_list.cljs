(ns ventas.devcards.category-list
  (:require
   [devcards.core :refer-macros [defcard-rg]]
   [ventas.components.category-list :as components.category-list]))

(defn- category-list-wrapper []
  [components.category-list/category-list
   (for [n (range 4)]
     {:id (gensym)
      :name (random-uuid)
      :description "A sample description"})])

(defcard-rg regular-category-list
  "Regular category list"
  [category-list-wrapper]
  {:inspect-data true})

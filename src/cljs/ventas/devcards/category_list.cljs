(ns ventas.devcards.category-list
  (:require
   [devcards.core]
   [ventas.components.category-list :as components.category-list])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defcard-rg regular-category-list
  "Regular category list"
  components.category-list/category-list
  (for [n 4]
    {:id (gensym)
     :name (random-uuid)
     :description "A sample description"})
  {:inspect-data true})
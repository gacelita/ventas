(ns ventas.plugins.featured-products.core
  (:require [ventas.plugin :as plugin]))

(defmethod plugin/widget :plugins.featured-products/list [_]
  [:ul
   [:li "Product A"]
   [:li "Product C"]])


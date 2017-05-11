(ns ventas.plugins.featured-products.core)

(defmethod ventas.plugin/widget :plugins.featured-products/list
  (fn []
    [:ul
      [:li "Product A"]
      [:li "Product B"]]))

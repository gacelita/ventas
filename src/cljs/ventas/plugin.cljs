(ns ventas.plugin)

(defmulti widget
  "Widgets multimethod"
  (fn [name widget] name))
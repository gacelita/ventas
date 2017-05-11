(ns ventas.plugin)

(defmulti widget
  "Widgets multimethod" [name]
  (fn [name] name))

(defmulti action
  "Actions multimethod" [name]
  (fn [name] name))

(defmulti filter
  "Filters multimethod" [name]
  (fn [name] name))

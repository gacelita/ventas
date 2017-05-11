(ns ventas.plugin
  (:refer-clojure :exclude [filter]))

(defmulti action
  "Actions multimethod"
  (fn [name action] name))

(defmulti filter
  "Filters multimethod"
  (fn [name filter] name))
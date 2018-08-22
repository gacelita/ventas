(ns ventas.dev
  "Development-only utilities"
  (:require
   [cljs.test]
   [ventas.core-test]))

(defn run-tests []
  (cljs.test/run-all-tests #"ventas.*?\-test"))

(ns ventas.dev
  (:require
   [cljs.test]
   [ventas.core-test]))

(defn run-tests []
  (cljs.test/run-all-tests #"ventas.*?\-test"))
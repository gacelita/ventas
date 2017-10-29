(ns ventas.example-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]))

(deftest example-passing-test
  (is (= 1 1)))

(deftest example-failing-test
  (is (= 1 0)))
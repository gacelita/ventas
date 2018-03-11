(ns ventas.config-test
  (:refer-clojure :exclude [set])
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.config :as sut]))

(deftest config-setting
  (sut/set :example :value)
  (is (= (sut/get :example) :value)))

(deftest config-setting-with-vector
  (sut/set [:another-example :a] :value)
  (is (= (sut/get :another-example :a) :value)))
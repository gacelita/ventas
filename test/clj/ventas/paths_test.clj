(ns ventas.paths-test
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.paths :as sut]))

(deftest resolve
  (testing "resolve single path"
    (is (= (sut/resolve ::sut/project-resources) "resources")))
  (testing "resolve compound path"
    (is (= (sut/resolve ::sut/rendered) "storage/rendered"))))

(deftest path->resource
  (is (= (sut/path->resource "resources/test") "test")))

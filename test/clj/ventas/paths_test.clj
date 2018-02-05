(ns ventas.paths-test
  (:refer-clojure :exclude [resolve])
  (:require
   [ventas.paths :as sut]
   [clojure.test :refer [deftest is testing]]))

(deftest resolve
  (testing "resolve single path"
    (is (= (sut/resolve ::sut/project-resources) "resources")))
  (testing "resolve compound path"
    (is (= (sut/resolve ::sut/public) "resources/public"))))

(deftest path->resource
  (is (= (sut/path->resource "resources/test") "test")))

(ns ventas.database-test
  (:require [clojure.test :refer :all]
            [ventas.database :as sut]))

(deftest enum-values
  (testing "common usage"
    (is (set? (sut/enum-values "schema.type")))))
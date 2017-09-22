(ns ventas.database-test
  (:require [clojure.test :refer [deftest testing is]]
            [ventas.database :as sut]))

(deftest enum-values
  (testing "common usage"
    (is (set? (sut/enum-values "schema.type")))))
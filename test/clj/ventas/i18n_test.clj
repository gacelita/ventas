(ns ventas.i18n-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.i18n :as sut]))

(deftest i18n
  (is (= (sut/i18n :en_US ::sut/test-value)
         "Test value")))
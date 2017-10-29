(ns ventas.common.util-test
  #? (:cljs (:require-macros [cljs.test :refer [is deftest testing]]))
  (:require [ventas.common.util :as sut]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test])))

(deftest read-keyword
  (is (= :keyword (sut/read-keyword ":keyword")))
  (is (= :test/keyword (sut/read-keyword ":test/keyword"))))

(deftest map-values
  (is (= {:a 2 :b 3} (sut/map-values inc {:a 1 :b 2}))))

(deftest deep-merge
  (testing "deep-merge merges maps recursively"
    (is (= (sut/deep-merge
            {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
            {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
           {:a {:b {:z 3, :c 2, :d {:z 9, :x 1, :y 2}}, :e 100}, :f 4}))))

(def example-input-message {:user {:roles [sut/set-identifier ":user.role/administrator" ":user.role/user"]}})

(def example-output-message {:user {:roles #{:user.role/administrator :user.role/user}}})

(deftest process-input-message
  (is (= example-output-message
         (sut/process-input-message example-input-message))))

(deftest process-output-message
  (is (= example-input-message
         (sut/process-output-message example-output-message))))
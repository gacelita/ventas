(ns ventas.utils.jar-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.string :as str]
   [ventas.utils.jar :as sut]))

(def example-jar "org/clojure/clojure/1.9.0/clojure-1.9.0.jar")

(deftest list-resources
  (is (contains? (->> (clojure.java.classpath/classpath-jarfiles)
                      (filter #(str/includes? (.getName %) example-jar))
                      (first)
                      (sut/list-resources)
                      (set))
                 "META-INF/maven/org.clojure/clojure/pom.xml")))

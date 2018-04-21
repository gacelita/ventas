(ns ventas.utils.jar-test
  (:require
   [clojure.java.classpath :as classpath]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [ventas.utils.jar :as sut]))

(def example-jar "org/clojure/clojure/1.9.0/clojure-1.9.0.jar")

(deftest list-resources
  (is (contains? (->> (classpath/classpath-jarfiles)
                      (filter #(str/includes? (.getName %) example-jar))
                      (first)
                      (sut/list-resources)
                      (set))
                 "META-INF/maven/org.clojure/clojure/pom.xml")))

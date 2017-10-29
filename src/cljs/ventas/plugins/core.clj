(ns ventas.plugins.core
  (:require
   [clojure.string :as str]))

(defmacro require-plugins []
  (let [names (->> (all-ns)
                   (map #(str (.getName %)))
                   (filter #(str/includes? % "ventas.plugins"))
                   (filter #(not (= "ventas.plugins.core" %)))
                   (map symbol))]
    `(do
       ~@(for [name names]
           `(~'require '[~name])))))
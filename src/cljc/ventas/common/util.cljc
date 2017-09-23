(ns ventas.common.util
  (:require
    [clojure.string :as s]))

(defn map-kv [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn deep-merge
  "Like merge, but merges maps recursively.
   See: https://dev.clojure.org/jira/browse/CLJ-1468"
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn process-input-message [message]
  "Properly decode keywords"
  (map-kv (fn [v] (cond
                    (and (string? v) (s/starts-with? v ":")) (keyword (s/replace v #"\:" ""))
                    (map? v) (process-input-message v)
                    :else v)) message))

(defn process-output-message [message]
  "Properly encode keywords"
  (map-kv (fn [v] (cond 
                    (keyword? v) (str v) 
                    (map? v) (process-output-message v)
                    :else v)) message))


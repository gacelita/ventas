(ns ventas.common.util
  (:require
    [clojure.string :as str]))

(defn map-values
  "Like using `map` over the values of a map, leaving the keys intact"
  [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn deep-merge
  "Like merge, but merges maps recursively.
   See: https://dev.clojure.org/jira/browse/CLJ-1468"
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn process-input-message
  "Properly decode keywords"
  [message]
  (cond
    (map? message) (map-values process-input-message message)
    (vector? message) (map process-input-message message)
    (and (string? message) (str/starts-with? message ":")) (keyword (str/replace message #"\:" ""))
    :else message))

(defn process-output-message
  "Properly encode keywords"
  [message]
  (cond
    (map? message) (map-values process-output-message message)
    (vector? message) (map process-output-message message)
    (keyword? message) (str message)
    :else message))


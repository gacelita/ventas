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

(defn read-keyword [str]
  (keyword (str/replace str #"\:" "")))

(def set-identifier "__set")

(defn process-input-message
  "Properly decode keywords and sets"
  [message]
  (cond
    (map? message)
      (map-values process-input-message message)
    (string? message)
      (if (str/starts-with? message ":")
        (read-keyword message)
        message)
    (vector? message)
      (if (= (first message) set-identifier)
        (set (map process-input-message (rest message)))
        (map process-input-message message))
    :else message))

(defn process-output-message
  "Properly encode keywords and sets"
  [message]
  (cond
    (map? message) (map-values process-output-message message)
    (vector? message) (map process-output-message message)
    (keyword? message) (str message)
    (set? message) (vec (concat [set-identifier] (map process-output-message message)))
    :else message))


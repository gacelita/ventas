(ns ventas.common.util
  (:require
    [clojure.string :as s]))

(defn map-kv [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

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


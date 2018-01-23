(ns ventas.common.utils
  #?(:clj
     (:require
      [clojure.string :as str]))
  #?(:cljs
     (:require
      [clojure.string :as str]
      [cljs.reader :as reader]
      [cognitect.transit :as transit])))

(defn map-keys [f m]
  (->> m
       (map (fn [[k v]]
              [(f k) v]))
       (into {})))

(defn map-vals [f m]
  (->> m
       (map (fn [[k v]]
              [k (f v)]))
       (into {})))

(defn map-kv [f m]
  (->> m
       (map (fn [[k v]]
              (f k v)))
       (into {})))

(defn filter-vals
  [pred m]
  (->> m
       (filter (fn [[k v]] (pred v)))
       (into {})))

(defn filter-empty-vals [m]
  (filter-vals (fn [v]
                 (not (nil? v)))
               m))

(defn find-first
  "Finds the first value from coll that satisfies pred.
  Returns nil if it doesn't find such a value."
  [pred coll]
  {:pre [(ifn? pred) (coll? coll)]}
  (some #(when (pred %) %) coll))

(defn find-index [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))

(defn deep-merge
  "Like merge, but merges maps recursively.
   See: https://dev.clojure.org/jira/browse/CLJ-1468"
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn group-by-keyword
  "Same as group-by but dissocs the keyword used for grouping the items."
  [kw coll]
  {:pre [(keyword? kw)]}
  (->> coll
       (group-by kw)
       (map-vals
        (fn [term]
          (->> term (map #(dissoc % kw)))))))

(defn update-when-some [m k f]
  (if (get m k)
    (update m k f)
    m))

(defn update-in-when-some [m ks f]
  (if (get-in m ks)
    (update-in m ks f)
    m))

(defn index-by
  "Indexes a collection using the given keyword as key"
  [keyword coll]
  (->> coll
       (map (fn [item]
              [(get item keyword) (dissoc item keyword)]))
       (into {})))

(defn read-keyword [str]
  (keyword (str/replace str #"\:" "")))

(def ^:private set-identifier "__set")

(defn str->bigdec [v]
  #?(:clj (bigdec v))
  #?(:cljs (transit/bigdec v)))

(defn bigdec->str [v]
  #?(:clj (str v))
  #?(:cljs (reader/read-string (.-rep v))))

(defn process-input-message
  "Properly decode keywords and sets"
  [message]
  (cond
    (map? message)
      (map-kv (fn [k v]
                [(process-input-message k)
                 (process-input-message v)])
              message)
    (string? message)
      (cond
        (str/starts-with? message ":")
          (read-keyword message)
        :else message)
    (sequential? message)
      (if (= (first message) set-identifier)
        (set (map process-input-message (rest message)))
        (map process-input-message message))
    :else message))

(defn process-output-message
  "Properly encode keywords and sets"
  [message]
  (cond
    (map? message) (map-kv (fn [k v]
                             [(process-output-message k)
                              (process-output-message v)])
                           message)
    (sequential? message) (map process-output-message message)
    (keyword? message) (str message)
    (set? message) (vec (concat [set-identifier] (map process-output-message message)))
    :else message))


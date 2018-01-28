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
  "Maps only the keys of a map"
  (->> m
       (map (fn [[k v]]
              [(f k) v]))
       (into {})))

(defn map-vals [f m]
  "Maps only the values of a map"
  (->> m
       (map (fn [[k v]]
              [k (f v)]))
       (into {})))

(defn map-kv
  "Syntax sugar for map->map transformations"
  [f m]
  (->> m
       (map (fn [[k v]]
              (f k v)))
       (into {})))

(defn map-leaves
  "Applies f to all 'leaves' (anything that is not sequential or a map)"
  [f v]
  (cond
    (map? v)
    (->> v
         (map (fn [[k v]]
                [(map-leaves f k)
                 (map-leaves f v)]))
         (into {}))

    (sequential? v)
    (map #(map-leaves f %) v)

    :default
    (f v)))

(defn remove-nil-vals
  "Removes nil values from a map"
  [m]
  (->> m
       (remove (fn [[k v]] (nil? v)))
       (into {})))

(defn find-first
  "Finds the first value from coll that satisfies pred.
   Returns nil if it doesn't find such a value."
  [pred coll]
  {:pre [(ifn? pred) (coll? coll)]}
  (some #(when (pred %) %) coll))

(defn find-index
  "Returns the index of the first value in `coll` that satisfies `pred`"
  [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))

(defn deep-merge
  "Like merge, but merges maps recursively.
   See: https://dev.clojure.org/jira/browse/CLJ-1468"
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn group-by-keyword
  "Same as group-by but dissocs the keyword used for grouping the items"
  [kw coll]
  {:pre [(keyword? kw)]}
  (->> coll
       (group-by kw)
       (map-vals
        (fn [term]
          (->> term (map #(dissoc % kw)))))))

(defn update-when-some
  "Like `update` but does not do anything if the value does not exist or is nil"
  [m k f]
  (if (get m k)
    (update m k f)
    m))

(defn update-in-when-some [m ks f]
  "See `update-when-some`"
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

(defn tree-by
  "Creates a recursive structure from a flat structure"
  [id-key parent-key coll & [parent]]
  (->> coll
       (filter #(if parent
                  (= parent (get % parent-key))
                  (not (get % parent-key))))
       (map (fn [root]
              [root (tree-by id-key parent-key coll (get root id-key))]))
       (into {})))

(defn read-keyword [str]
  "Makes a keyword from a string beginning with `:`"
  (keyword (str/replace str #"\:" "")))

(def ^:private set-identifier "__set")

(def ^:private vector-identifier "__vector")

(defn str->bigdec [v]
  #?(:clj (bigdec v))
  #?(:cljs (transit/bigdec v)))

(defn bigdec->str [v]
  #?(:clj (str v))
  #?(:cljs (reader/read-string (.-rep v))))

(defn process-input-message
  "Properly decode keywords, sets and vectors.
   Used for json communication between client and server, to allow using keywords and sets"
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
      (cond
        (= (first message) set-identifier)
        (set (map process-input-message (rest message)))

        (= (first message) vector-identifier)
        (mapv process-input-message (rest message))

        :default
        (map process-input-message message))
    :default message))

(defn process-output-message
  "Properly encode keywords, sets and vectors.
   Used for json communication between client and server, to allow using keywords and sets"
  [message]
  (cond
    (map? message)
      (map-kv (fn [k v]
                [(process-output-message k)
                 (process-output-message v)])
              message)
    (sequential? message)
      (if (vector? message)
        (concat [vector-identifier] (map process-output-message message))
        (map process-output-message message))
    (keyword? message)
      (str message)
    (set? message)
      (vec (concat [set-identifier] (map process-output-message message)))
    :default message))


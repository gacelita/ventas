(ns ventas.common.utils
  #?(:clj
     (:require
      [clojure.string :as str]))
  #?(:cljs
     (:require
      [clojure.string :as str]
      [cljs.reader :as reader]
      [cognitect.transit :as transit])))

(defn mapm
  "Like clojure.core/mapv, but creates a map"
  ([f coll]
   (-> (reduce (fn [m o] (let [[k v] (f o)] (assoc! m k v))) (transient {}) coll)
       persistent!))
  ([f c1 c2]
   (into {} (map f c1 c2)))
  ([f c1 c2 c3]
   (into {} (map f c1 c2 c3)))
  ([f c1 c2 c3 & colls]
   (into {} (apply map f c1 c2 c3 colls))))

(defn map-keys [f m]
  "Maps only the keys of a map"
  (reduce-kv
   (fn [m k v]
     (assoc m (f k) v))
   {}
   m))

(defn map-vals [f m]
  "Maps only the values of a map"
  (reduce-kv
   (fn [m k v]
     (assoc m k (f v)))
   {}
   m))

(defn map-kv
  "Syntax sugar for map->map transformations"
  [f m]
  (reduce-kv
   (fn [m k v]
     (let [[k v] (f k v)]
       (assoc m k v)))
   {}
   m))

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

(defn remove-index
  [idx coll]
  (keep-indexed (fn [curr-idx itm]
                  (when (not= curr-idx idx)
                    itm))
                coll))

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

(defn into-n [& vs]
  "Into but accepts any number of input vectors"
  (reduce into [] vs))

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

(defn str->bigdec [v]
  #?(:clj (bigdec v))
  #?(:cljs (transit/bigdec (str v))))

(defn bigdec->str [v]
  #?(:clj (str v))
  #?(:cljs (when v (reader/read-string (.-rep v)))))
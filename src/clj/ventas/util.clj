(ns ventas.util)

(defn filter-vals
  [pred m]
  (into {} (filter (fn [[k v]] (pred v))
                   m)))

(defn filter-empty-vals [m] (filter-vals (fn [v] (not (nil? v))) m))

(defn dequalify-keywords [m]
  (into {}
    (for [[k v] m]
      [(keyword (name k)) v])))

(defn qualify-keyword [kw ns]
  (keyword (name ns) (name kw)))

(defn qualify-map-keywords [ks ns]
	"Qualifies the keywords used as keys in a map.
	 Accepts a map with keywords as keys and a namespace represented as keyword."
	(into {}
		(for [[k v] ks]
			[(qualify-keyword k ns) v])))


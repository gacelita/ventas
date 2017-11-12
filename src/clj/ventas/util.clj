(ns ventas.util
  (:require [io.aviso.ansi :as clansi]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]
            [slingshot.slingshot :refer [throw+]])
  (:import [java.io File]))

(defn filter-vals
  [pred m]
  (into {} (filter (fn [[k v]] (pred v))
                   m)))

(defn find-first
  "Finds the first value from coll that satisfies pred.
  Returns nil if it doesn't find such a value."
  [pred coll]
  {:pre [(ifn? pred) (coll? coll)]}
  (some #(when (pred %) %) coll))

(defn chan? [v]
  (satisfies? clojure.core.async.impl.protocols/Channel v))

(defn filter-empty-vals [m] (filter-vals (fn [v] (not (nil? v))) m))

(defn dequalify-keywords [m]
  (into {}
    (for [[k v] m]
      [(keyword (name k)) v])))

(defn qualify-keyword [kw ns]
  (keyword (name ns) (name kw)))

(defn qualify-map-keywords
	"Qualifies the keywords used as keys in a map.
	 Accepts a map with keywords as keys and a namespace represented as keyword."
  [ks ns]
	(into {}
		(for [[k v] ks]
			[(qualify-keyword k ns) v])))

(defn print-info [str]
  (println (clansi/green str)))

(defn print-error [str]
  (println (clansi/red str)))

(defn find-files*
  "Find files in `path` by `pred`."
  [path pred]
  (filter pred (-> path io/file file-seq)))

(defn find-files
  "Find files matching given `pattern`."
  [path pattern]
  (find-files* path #(re-matches pattern (.getName ^File %))))

(defn transform [data xforms]
  (let [f (apply comp (reverse xforms))]
    (f data)))

(defn check [& args]
  "([spec x] [spec x form])
     Returns true when x is valid for spec. Throws an Error if validation fails."
  (if (apply spec/valid? args)
    true
    (do
      (throw (Exception. (with-out-str (apply expound/expound args)))))))

(defn spec-exists? [kw]
  (try
    (spec/describe kw)
    true
    (catch Exception e
      false)))

(defn update-if-exists [thing kw update-fn]
  (if (get thing kw)
    (update thing kw update-fn)
    thing))

(defn has-duplicates? [xs]
  (not= (count (distinct xs)) (count xs)))
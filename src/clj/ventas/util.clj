(ns ventas.util
  (:require [io.aviso.ansi :as clansi]
            [clojure.java.io :as io])
  (:import [java.io File]))

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
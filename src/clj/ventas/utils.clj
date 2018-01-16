(ns ventas.utils
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [expound.alpha :as expound]
   [clojure.core.async :as core.async :refer [go-loop go <! >! chan]])
  (:import [java.io File]))

(defn chan? [v]
  (satisfies? clojure.core.async.impl.protocols/Channel v))

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

(defn find-files*
  "Find files in `path` by `pred`."
  [path pred]
  (filter #(and (pred %)
                (not (.isDirectory %))) (-> path io/file file-seq)))

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

(defmacro ns-kw
  "Takes a string or a keyword. Returns a keyword where the ns is the caller ns
   and the name is the given string, or the name of the given keyword."
  [input]
  (let [caller-ns (str (:name (:ns &env)))]
    `(~'keyword ~caller-ns ~input)))

(defn bigdec?
  "Return true if x is a BigDecimal.
   This function was already in 1.9 alphas but it was removed :("
  [x] (instance? java.math.BigDecimal x))

(defn ->number [v]
  (try
    (Long. v)
    (catch Exception e
      nil)))

(defn batch [in out max-time max-count]
  (let [lim-1 (dec max-count)]
    (go-loop [buffer []]
      (let [[message ch] (core.async/alts! [in (core.async/timeout max-time)])]
        (cond
          (not= ch in)
          (do
            (core.async/>! out buffer)
            (recur []))

          (nil? message)
          (when (seq buffer)
            (core.async/>! out buffer))

          (= (count buffer) lim-1)
          (do
            (core.async/>! out (conj buffer message))
            (recur []))

          :else
          (recur (conj buffer message)))))))
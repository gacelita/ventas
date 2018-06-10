(ns ventas.utils
  (:require
   [clojure.core.async :as core.async :refer [<! >! chan go go-loop]]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [expound.alpha :as expound]
   [slingshot.slingshot :refer [throw+]]
   [taoensso.timbre :as timbre]
   [ventas.common.utils :as common.utils])
  (:import
   [clojure.lang IAtom]
   [java.io File]))

(defn chan? [v]
  (satisfies? clojure.core.async.impl.protocols/Channel v))

(defn atom? [v]
  (instance? IAtom v))

(defmacro swallow [& body]
  `(try (do ~@body)
        (catch Throwable e# nil)))

(defmacro interruptible-try [& body]
  (let [additional-exceptions (when (vector? (first body))
                                (->> (first body)
                                     (map (fn [ex]
                                            `(catch ~ex ~'_
                                               (.interrupt (Thread/currentThread)))))))
        body (if (vector? (first body))
               (rest body)
               body)]
    `(try
       ~@body
       (catch InterruptedException ~'_
         (.interrupt (Thread/currentThread)))
       ~@additional-exceptions
       (catch Throwable ~'e
         (timbre/error (class ~'e) (.getMessage ~'e))))))

(defn spec-exists? [v]
  (swallow
   (spec/form v)))

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

(defn dequalify-keywords [m]
  (mapm (fn [[k v]]
          [(keyword (name k)) v])
        m))

(defn qualify-keyword [kw ns]
  (keyword (name ns) (name kw)))

(defn qualify-map-keywords
  "Qualifies the keywords used as keys in a map.
	 Accepts a map with keywords as keys and a namespace represented as keyword."
  [ks ns]
  (mapm (fn [[k v]]
          [(qualify-keyword k ns) v])
        ks))

(defn find-files*
  "Find files in `path` by `pred`."
  [path pred]
  (filter #(and (pred %)
                (not (.isDirectory %)))
          (-> path io/file file-seq)))

(defn find-files
  "Find files matching given `pattern`."
  [path pattern]
  (find-files* path
               #(re-matches pattern (.getName ^File %))))

(defn transform
  "Applies the given transformation functions to `data`"
  [data xforms]
  (let [f (apply comp (reverse xforms))]
    (f data)))

(defn check [& args]
  "([spec x] [spec x form])
     Returns true when x is valid for spec. Throws an Error if validation fails."
  (if (apply spec/valid? args)
    true
    (throw+ {:type ::spec-invalid
             :explanation (with-out-str (apply expound/expound args))})))

(defn update-if-exists [thing kw update-fn]
  (if (get thing kw)
    (update thing kw update-fn)
    thing))

(defn has-duplicates? [xs]
  (not= (count (distinct xs)) (count xs)))

(defmacro ns-kw
  "Takes a string or a keyword. Returns a keyword where the ns is the caller ns
   and the name is the given string, or the name of the given keyword.
   **ClojureScript only**"
  [input]
  (if-not (:ns &env)
    `(throw+ {:type ::unsupported-environment
              :message "This macro is cljs-only"})
    (let [caller-ns (str (:name (:ns &env)))]
      `(~'keyword ~caller-ns ~input))))

(defn bigdec?
  "Return true if x is a BigDecimal.
   This function was already in 1.9 alphas but it was removed :("
  [x] (instance? BigDecimal x))

(defn ->number [v]
  (swallow (Long. v)))

(def ^:deprecated into-n common.utils/into-n)

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
          (do
            (when (seq buffer)
              (core.async/>! out buffer))
            (core.async/close! out))

          (= (count buffer) lim-1)
          (do
            (core.async/>! out (conj buffer message))
            (recur []))

          :else
          (recur (conj buffer message)))))))
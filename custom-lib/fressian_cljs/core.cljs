(ns fressian-cljs.core
  (:require [clojure.string :as string])
  (:use [fressian-cljs.reader :only [read-object FressianReader]]
        [fressian-cljs.writer :only [write-object FressianWriter write-tag write-footer write-list]]
        [fressian-cljs.adler32 :only [make-adler32]]
        [fressian-cljs.defs :only [TaggedObject create-interleaved-index-hop-map]]))

(defn load-string [s]
  (throw (js/Error. "load-string not supported.")))

(defn- record-map-constructor-name
  "Return the map constructor for a record"
  [rname]
  (let [comps (string/split (str rname) #"\.")]
    (str (->> (butlast comps) (map #(string/replace % "_" "-"))
           (string/join "."))
      "/map->" (last comps))))

(def cljs-read-handler
  { "bigint" (fn [reader tag component-count]
               (read-object reader))
    "byte"   (fn [reader tag component-count]
               (read-object reader))
    "record" (fn [reader tag component-count]
               (let [ rname (read-object reader)
                      rmap  (read-object reader)]
                 (if-let [rcons (load-string (record-map-constructor-name rname))]
                   (rcons rmap)
                   (TaggedObject. "record" (into-array js/Object [rname rmap]) nil))))

    "char"   (fn [reader tag component-count]
               (.fromCharCode js/String (read-object reader)))

    "ratio"  (fn [reader tag component-count]
               (/ (js/parseInt (read-object reader))
                 (js/parseInt  (read-object reader))))

    "key"    (fn [reader tag component-count]
              (keyword (read-object reader) (read-object reader)))

    "sym"    (fn [reader tag component-count]
              (symbol (read-object reader) (read-object reader)))

    "map"    (fn [reader tag component-count]
              (let [kvs (read-object reader)]
                (apply hash-map kvs)))

    "vec"    (fn [reader tag component-count]
               (vec (read-object reader)))})

(def cljs-write-handler
  { cljs.core/PersistentVector
    { "vec"
      (fn [wtr v]
        (write-tag wtr "vec" 1)
        (write-list wtr (seq v)))}})

(defmulti create-reader type)
(defmethod create-reader js/ArrayBuffer [buf & {:keys [handlers]}]
  (atom (FressianReader. buf 0
          (or handlers cljs-read-handler)
          []
          [])))

(defmethod create-reader js/Blob [buf & {:keys [handlers]}]
  (throw "Blob FressianReader has been implemented yet."))

(defn read [readable & options]
  (read-object (apply create-reader readable options)))

(defn create-writer [& {:keys [handlers]}]
  (let [buffer (js/ArrayBuffer. 65536)]
    (atom (FressianWriter. buffer 0
            (or handlers cljs-write-handler) (make-adler32)
            (create-interleaved-index-hop-map 16)
            (create-interleaved-index-hop-map 16)))))

(defn ^js/Int8Array write [obj & options]
  (let [{:keys [footer?]} (when options (apply hash-map options))
         writer (apply create-writer options)]
    (write-object writer obj)
    (when footer?
      (write-footer writer))
    (-> (js/Int8Array. (:buffer @writer))
        (.subarray 0 (:index @writer)))))


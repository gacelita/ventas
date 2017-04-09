(ns fressian-cljs.reader
  (:use [fressian-cljs.defs :only [codes TaggedObject StructType]]
        [fressian-cljs.fns :only [read-utf8-chars expected byte-array-to-uuid]])
  (:require [goog.string :as gstring]
            [goog.string.format]))

(declare read-object)
(declare read-int)
(declare read-boolean)
(declare read-float)
(declare read-double)

(declare internal-read-int)
(declare read)

(defrecord FressianReader [buffer index handlers priority-cache struct-cache])

(def standard-extension-hadlers
  { "set"   (fn [r tag component-count]
              (set (read-object r)))
    "map"   (fn [r tag component-count]
              (apply hash-map (read-object r)))
    "int[]" (fn [r tag component-count]
              (let [ size (read-int r)
                     result (make-array size)]
                (doseq [n (range 0 size)]
                  (aset result n (read-int r)))
                result))
    "long[]" (fn [r tag component-count]
              (let [ size (read-int r)
                     result (make-array size)]
                (doseq [n (range 0 size)]
                  (aset result n (read-int r)))
                result))
    "boolean[]" (fn [r tag component-count]
                  (let [ size (read-int r)
                         result (make-array size)]
                    (doseq [n (range 0 size)]
                      (aset result n (read-boolean r)))
                    result))
    "float[]" (fn [r tag component-count]
                (let [ size (read-int r)
                       result (make-array size)]
                  (doseq [n (range 0 size)]
                    (aset result n (read-float r)))
                  result))
    "double[]" (fn [r tag component-count]
                 (let [ size (read-int r)
                        result (make-array size)]
                   (doseq [n (range 0 size)]
                     (aset result n (read-double r)))
                   result))
    "Object[]" (fn [r tag component-count]
                 (let [ size (read-int r)
                        result (make-array size)]
                   (doseq [n (range 0 size)]
                     (aset result n (read-object r)))
                   result))
    "uuid"     (fn [r tag component-count]
                 (let [buf (read-object r)]
                   (when-not (= (. buf -length) 16)
                     (throw (str "Invalid uuid buffer size: " (count buf))))
                   (byte-array-to-uuid buf)))
    "regex"    (fn [r tag component-count]
                   (re-pattern (read-object r)))
    "uri"      (fn [r tag component-count]
                 (read-object r))
    "bigint"   (fn [r tag component-count]
                 ())
    "bigdec"   (fn [r tag component-count]
                 ())
    "inst"     (fn [r tag component-count]
                 (let [d (js/Date.)
                       tm (read-int r)]
                   (.setTime d tm)
                   d))
    })

(def core-handlers
  {:list #(apply list %)
   :bytes identity
   :double identity
   :float identity})

(def under-construction (js/Object.))

(defn- read-and-cache-object [reader]
  (let [o (read-object reader)]
    (swap! reader update-in [:priority-cache] conj o)
    o))

(defn- lookup-cache [cache index]
  (if (< index (count cache))
    (let [result (nth cache index)]
      (if (= result under-construction)
        (throw (js/Error. "Unable to resolve circular reference in cache"))
        result))
    (throw (js/Error. (str "Requested object beyond end of cache at " index)))))

(defn- reset-caches [reader]
  (swap! reader assoc :priority-cache nil)
  (swap! reader assoc :struct-cache nil))

(defn- read-next-code [reader]
  (let [code (js/Uint8Array. (:buffer @reader) (:index @reader) 1)]
    (swap! reader update-in [:index] inc)
    (aget code 0)))

(defn read-fully [reader length]
  (let [buf (js/Uint8Array. (:buffer @reader) (:index @reader) length)]
    (swap! reader update-in [:index] + length)
    buf))

(defn read-raw-byte [reader]
  (let [result (some-> (js/Uint8Array. (:buffer @reader) (:index @reader) 1)
                       (aget 0))]
    (if (< result 0)
      (throw (js/Error. "EOF"))
      (do
        (swap! reader update-in [:index] inc)
        result))))

(defn << [x y]
  (* x (.pow js/Math 2 y)))

(defn read-raw-int8 [reader]
  (read-raw-byte reader))

(defn read-raw-int16 [reader]
  (+ (<< (read-raw-byte reader) 8)
     (read-raw-byte reader)))

(defn read-raw-int24 [reader]
  (+ (<< (read-raw-byte reader) 16)
     (<< (read-raw-byte reader)  8)
     (read-raw-byte reader)))

(defn read-raw-int32 [reader]
  (+ (<< (read-raw-byte reader) 24)
     (<< (read-raw-byte reader) 16)
     (<< (read-raw-byte reader)  8)
     (read-raw-byte reader)))

(defn read-raw-int40 [reader]
  (+ (<< (read-raw-byte reader) 32)
     (read-raw-int32 reader)))

(defn read-raw-int48 [reader]
  (+ (<< (read-raw-byte reader) 40)
     (read-raw-int40 reader)))

(defn read-raw-int64 [reader]
  (+ (<< (read-raw-byte reader) 56)
     (<< (read-raw-byte reader) 48)
     (read-raw-int48 reader)))

(defn read-raw-float [reader]
  (let [f32buf (js/Float32Array. 1)
        u8buf  (js/Uint8Array. (. f32buf -buffer))]
    (dotimes [i 4]
      (let [b (read-raw-byte reader)]
        (aset u8buf i b)))
    (aget f32buf 0)))

(defn read-raw-double [reader]
  (let [buf (js/ArrayBuffer. 8)
        h (read-raw-int32 reader)
        l (read-raw-int32 reader)]
    (aset (js/Int32Array. buf) 0 h)
    (aset (js/Int32Array. buf) 1 l)
    (aget (js/Float64Array. buf) 0)))

(defn- internal-read-int32 [reader] (internal-read-int reader))

(defn- internal-read-double [reader code]
  (condp = code
    (codes :double)   (read-raw-double reader)
    (codes :double-0) 0.0
    (codes :double-1) 1.0
    (let [o (read reader code)]
      (if (= (type o) js/Number)
        o
        (throw (js/Error. (expected "double" code o)))))))

(defn read-boolean [reader]
  (let [code (read-next-code reader)]
    (cond
      (= code (codes :true)) true
      (= code (codes :false)) false
      :default (let [res (read reader code)]
                 (if (= js/Boolean (type res))
                   res
                   (throw (js/Error. (expected "boolean" code res))))))))

(defn read-int [reader]
  (internal-read-int reader))

(defn read-double [reader]
  (let [code (read-next-code reader)]
    (internal-read-double reader code)))

(defn read-float [reader]
  (let [code (read-next-code reader)]
    (cond
      (= code (codes :float)) (read-raw-float reader)
      :default (let [o (read reader code)]
                 (if (= (type o) js/Number)
                   o
                   (throw (js/Error. (expected "float" code o))))))))

(defn- read-int32 [reader] (read-int reader))
(defn- read-count [reader] (read-int32 reader))

(defn- internal-read-string [reader length]
  (let [buf (read-fully reader length)]
    (read-utf8-chars buf 0 length)))

(defn- internal-read-chunked-string [reader length]
  (let [buf (atom (internal-read-string reader length))]
    (loop [code (read-next-code reader)]
      (cond
       (<= (codes :string-packed-length-start) code (+ (codes :string-packed-length-start) 7))
       (internal-read-string reader (- code (codes :string-packed-length-start)))

       (= code (codes :string)) (internal-read-string reader (read-count reader))

       (= code (codes :string-chunk))
       (internal-read-string reader (read-count reader))))))

(defn- internal-read-bytes [reader length]
  (read-fully reader length))

(defn- internal-read-chunked-bytes [reader])

(defn- read-closed-list [reader]
  (let [objects (atom [])]
    (loop [code (read-next-code reader)]
      (if-not (= code (codes :end-collection))
        (swap! objects conj (read reader code))
        (recur (read-next-code reader))))
    @objects))

(defn- read-open-list [reader]
  (let [objects (atom [])]
    (try
      (loop [code (read-next-code reader)]
        (if-not (= code (codes :end-collection))
          (swap! objects conj (read reader code))
          (recur (read-next-code reader))))
      (catch js/Error ex))
    @objects))

(defn- read-objects [reader length]
  (for [x (range 0 length)]
    (read-object reader)))

(defn- internal-read-list [reader length]
  ((core-handlers :list) (read-objects reader length)))

(defn- handle-struct [reader tag fields]
  (if-let [h (or (get (get @reader :handlers) tag)
                 (get standard-extension-hadlers tag))]
    (h reader tag fields)
    (TaggedObject. tag (read-objects reader fields) nil)))

(defn- validate-footer [reader length magic-from-stream]
  (when-not (= magic-from-stream (codes :footer-magic))
    (throw (js/Error. (gstring/format "Invalid footer magic, expected %d got %d" (codes :footer-magic) magic-from-stream))))
  (let [length-from-stream (read-raw-int32 reader)]
    (when-not (= length length-from-stream)
      (throw (js/Error. (gstring/format "Invalid footer length, expected %d got %d" length length-from-stream)))))
  (reset-caches reader))

(defn- read [reader code]
  (cond
   (= code 0xFF) -1
   (<= 0x00 code 0x3F) (bit-and code 0xFF)
   (<= 0x40 code 0x5F) (+ (<< (- code (codes :int-packed-2-zero)) 8)
                          (read-raw-int8 reader))
   (<= 0x60 code 0x6F) (+ (<< (- code (codes :int-packed-3-zero)) 16)
                          (read-raw-int16 reader))
   (<= 0x70 code 0x73) (+ (<< (- code (codes :int-packed-4-zero)) 24)
                          (read-raw-int24 reader))
   (<= 0x74 code 0x77) (+ (<< (- code (codes :int-packed-5-zero)) 32)
                          (read-raw-int32 reader))
   (<= 0x78 code 0x7B) (+ (<< (- code (codes :int-packed-6-zero)) 40)
                          (read-raw-int40 reader))
   (<= 0x7C code 0x7F) (+ (<< (- code (codes :int-packed-7-zero)) 48)
                          (read-raw-int48 reader))
   (= code (codes :put-priority-cache)) (read-and-cache-object reader)
   (= code (codes :get-priority-cache)) (lookup-cache (:priority-cache @reader) (read-int32 reader))

   (<= (codes :priority-cache-packed-start) code (+ (codes :priority-cache-packed-start) 31))
   (lookup-cache (:priority-cache @reader) (- code (codes :priority-cache-packed-start)))

   (<= (codes :struct-cache-packed-start) code (+ (codes :struct-cache-packed-start) 15))
   (let [st (lookup-cache (:priority-cache @reader) (- code (codes :struct-cache-packed-start)))]
     (handle-struct reader (:tag st) (:fields st)))

   (= code (codes :map)) (handle-struct reader "map" 1)
   (= code (codes :set)) (handle-struct reader "set" 1)
   (= code (codes :uuid)) (handle-struct reader "uuid" 2)
   (= code (codes :regex)) (handle-struct reader "regex" 1)
   (= code (codes :uri)) (handle-struct reader "uri" 1)
   (= code (codes :bigint)) (handle-struct reader "bigint" 1)
   (= code (codes :bigdec)) (handle-struct reader "bigdec" 2)
   (= code (codes :inst)) (handle-struct reader "inst" 1)
   (= code (codes :sym)) (handle-struct reader "sym" 2)
   (= code (codes :key)) (handle-struct reader "key" 2)
   (= code (codes :int-array)) (handle-struct reader "int[]" 2)
   (= code (codes :long-array)) (handle-struct reader "long[]" 2)
   (= code (codes :float-array)) (handle-struct reader "float[]" 2)
   (= code (codes :boolean-array)) (handle-struct reader "boolean[]" 2)
   (= code (codes :double-array)) (handle-struct reader "double[]" 2)
   (= code (codes :object-array)) (handle-struct reader "Object[]" 2)

   (<= (codes :bytes-packed-length-start) code (+ (codes :bytes-packed-length-start) 7))
   (internal-read-bytes reader (- code (codes :bytes-packed-length-start)))

   (= code (codes :bytes)) (internal-read-bytes reader (read-count reader))
   (= code (codes :bytes-chunk)) (internal-read-chunked-bytes reader)

   (<= (codes :string-packed-length-start) code (+ (codes :string-packed-length-start) 7))
   (internal-read-string reader (- code (codes :string-packed-length-start)))

   (= code (codes :string)) (internal-read-string reader (read-count reader))
   (= code (codes :string-chunk)) (internal-read-chunked-string reader (read-count reader))

   (<= (codes :list-packed-length-start) code (+ (codes :list-packed-length-start) 7))
   (internal-read-list reader (- code (codes :list-packed-length-start)))

   (= code (codes :list)) (internal-read-list reader (read-count reader))
   (= code (codes :begin-closed-list)) ((core-handlers :list) (read-closed-list reader))
   (= code (codes :begin-open-list))   ((core-handlers :list) (read-open-list reader))

   (= code (codes :true)) true
   (= code (codes :false)) false

   (some #{(codes :double) (codes :double-0) (codes :double-1)} [code])
   (internal-read-double reader code)
   (= code (codes :float)) ((core-handlers :float) (read-raw-float reader))
   (= code (codes :int)) (read-raw-int64 reader)
   (= code (codes :null)) nil

   (= code (codes :footer))
   (let [length (dec (reader :index))
         magic-from-stream (+ (bit-shift-left code 24) (read-raw-int24 reader))]
     (validate-footer reader length magic-from-stream)
     (read-object reader))

   (= code (codes :structtype))
   (let [tag (read-object reader)
         fields (read-int32 reader)]
     (swap! reader update-in [:struct-cache] conj (StructType. tag fields))
     (handle-struct reader tag fields))

   (= code (codes :struct))
   (let [st (lookup-cache (:struct-cache @reader) (read-int32 reader))]
     (handle-struct reader (:tag st) (:fields st)))

   (= code (codes :reset-caches))
   (do
     (reset-caches reader)
     (read-object reader))

   :default (throw (js/Error. (expected "any" code)))))

(defn internal-read-int [reader]
  (let [code (read-next-code reader)]
    (cond
     (= code 0xFF) -1
     (<= 0x00 code 0x3F) (bit-and code 0xFF)
     (<= 0x40 code 0x5F) (+ (<< (- code (codes :int-packed-2-zero)) 8)
                            (read-raw-int8 reader))
     (<= 0x60 code 0x6F) (+ (<< (- code (codes :int-packed-3-zero)) 16)
                                 (read-raw-int16 reader))
     (<= 0x70 code 0x73) (+ (<< (- code (codes :int-packed-4-zero)) 24)
                                 (read-raw-int24 reader))
     (<= 0x74 code 0x77) (+ (<< (- code (codes :int-packed-5-zero)) 32)
                                 (read-raw-int32 reader))
     (<= 0x78 code 0x7B) (+ (<< (- code (codes :int-packed-6-zero)) 40)
                                 (read-raw-int40 reader))
     (<= 0x7C code 0x7F) (+ (<< (- code (codes :int-packed-7-zero)) 48)
                                 (read-raw-int48 reader))
     (= code (codes :int)) (read-raw-int64 reader)
     :default (let [o (read reader code)]
                (if (= js/Number (type o)) o
                  (throw (js/Error. (expected "int64" code o))))))))

(defn read-object [reader]
  (read reader (read-next-code reader)))


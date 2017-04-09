(ns fressian-cljs.defs)

(def codes
  { :priority-cache-packed-start 0x80
    :priority-cache-packed-end 0xA0
    :struct-cache-packed-start 0xA0
    :struct-cache-packed-end 0xB0
    :long-array    0xB0
    :double-array  0xB1
    :boolean-array 0xB2
    :int-array     0xB3
    :float-array   0xB4
    :object-array  0xB5
    :map    0xC0
    :set    0xC1
    :uuid   0xC3
    :regex  0xC4
    :uri    0xC5
    :bigint 0xC6
    :bigdec 0xC7
    :inst   0xC8
    :sym    0xC9
    :key    0xCA
    :get-priority-cache 0xCC
    :put-priority-cache 0xCD
    :precache 0xCE
    :footer 0xCF
    :footer-magic 0xCFCFCFCF
    :bytes-packed-length-start 0xD0
    :bytes-packed-length-end   0xD8
    :bytes-chunk 0xD8
    :bytes 0xD9
    :string-packed-length-start 0xDA
    :string-packed-length-end   0xE2
    :string-chunk 0xE2
    :string 0xE3
    :list-packed-length-start 0xE4
    :list-packed-length-end   0xEC
    :list 0xEC
    :begin-closed-list 0xED
    :begin-open-list   0xEE
    :structtype 0xEF
    :struct 0xF0
    :meta   0xF1
    :any    0xF4
    :true   0xF5
    :false  0xF6
    :null   0xF7
    :int    0xF8
    :float  0xF9
    :double 0xFA
    :double-0 0xFB
    :double-1 0xFC
    :end-collection 0xFD
    :reset-caches   0xFE
    :int-packed-1-start 0xFF
    :int-packed-1-end   0x40
    :int-packed-2-start 0x40
    :int-packed-2-zero  0x50
    :int-packed-2-end   0x60
    :int-packed-3-start 0x60
    :int-packed-3-zero  0x68
    :int-packed-3-end   0x70
    :int-packed-4-start 0x70
    :int-packed-4-zero  0x72
    :int-packed-4-end   0x74
    :int-packed-5-start 0x74
    :int-packed-5-zero  0x76
    :int-packed-5-end   0x78
    :int-packed-6-start 0x78
    :int-packed-6-zero  0x7A
    :int-packed-6-end   0x7C
    :int-packed-7-start 0x7C
    :int-packed-7-zero  0x7E
    :int-packed-7-end   0x80})

(def ranges
  { :packed-1-start     1
    :packed-1-end       64
    :packed-2-start     (js/parseInt "0xFFFFFFFFFFFFF000") 
    :packed-2-end       (js/parseInt "0x0000000000001000")
    :packed-3-start     (js/parseInt "0xFFFFFFFFFFF80000")
    :packed-3-end       (js/parseInt "0x0000000000080000")
    :packed-4-start     (js/parseInt "0xFFFFFFFFFE000000")
    :packed-4-end       (js/parseInt "0x0000000002000000")
    :packed-5-start     (js/parseInt "0xFFFFFFFE00000000")
    :packed-5-end       (js/parseInt "0x0000000200000000")
    :packed-6-start     (js/parseInt "0xFFFFFE0000000000")
    :packed-6-end       (js/parseInt "0x0000020000000000")
    :packed-7-start     (js/parseInt "0xFFFE000000000000")
    :packed-7-end       (js/parseInt "0x0002000000000000")

    :priority-cache-packed-end 32
    :struct-cache-packed-end   16
    :bytes-packed-length-end   8
    :string-packed-length-end   8
    :list-packed-length-end    8

    :byte-chunk-size    65535})

(def tag-to-code
  { "map"      (codes :map)
    "set"      (codes :set)
    "uuid"     (codes :uuid)
    "regex"    (codes :regex)
    "uri"      (codes :uri)
    "bigint"   (codes :bigint)
    "bigdec"   (codes :bigdec)
    "inst"     (codes :inst)
    "sym"      (codes :sym)
    "key"      (codes :key)
    "int[]"    (codes :int-array)
    "float[]"  (codes :float-array)
    "double[]" (codes :double-array)
    "long[]"   (codes :long-array)
    "boolean[]" (codes :boolean-array)
    "Object[]" (codes :object-array)})

(defrecord TaggedObject [tag value meta])

(defrecord StructType [tag fields])

(defprotocol InterleavedIndexHoppable
  (old-index [this k]))

(defrecord InterleavedIndexHopMap [ks hash-indexes]
  InterleavedIndexHoppable
  (old-index [_ k]
    (let [h (hash k)]
      (if-let [idx (get @hash-indexes h)]
        idx
        (do (swap! ks conj k)
            (swap! hash-indexes assoc h (dec (count @ks)))
            -1)))))

(defn create-interleaved-index-hop-map
  ([] (create-interleaved-index-hop-map 1024))
  ([capacity]
    (let [cap capacity]
      (->InterleavedIndexHopMap
        (atom [])
        (atom {})))))


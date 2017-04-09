(ns fressian-cljs.adler32)

(def *BASE* 65521)

(defprotocol Adler32Protocol
  (update! [_ b] [_ bs off len])
  (get-value [_])
  (reset [_]))

(defrecord Adler32 [value]
  Adler32Protocol
  (update! [_ b]
          (let [s1 (+ (bit-and @value 0xffff) (bit-and b 0xff))
                s2 (+ (bit-and (bit-shift-right @value 16) 0xffff) s1)]
            (reset! value
                    (bit-or (bit-shift-left (mod s2 *BASE*) 16)
                            (mod s1 *BASE*)))))
  (update! [_ bs off len]
          (doseq [i (range off (+ off len))]
            (update! _ (aget bs i))))
  (get-value [_] (bit-and @value 0xffffffff))
  (reset [_] (reset! value 1)))

(defn make-adler32 []
  (map->Adler32 {:value (atom 1)}))

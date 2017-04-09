(ns fressian-cljs.fns
  (:require [fressian-cljs.uuid :as uuid]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]))

(defn expected
  ([expected ch]
   (throw (gstring/format "expected %s at %d" expected ch)))
  ([expected ch got]
   (throw (gstring/format "expected %s at %d, got %s" expected ch got))))

(defn uuid-to-byte-array [uuid]
  (uuid/parse (.-uuid uuid)))

(defn- create-array-from-typed [array-buffer-view]
  (let [arr (make-array (. array-buffer-view -length))]
    (dotimes [n (count arr)]
      (aset arr n (aget array-buffer-view n)))
    arr))

(defn byte-array-to-uuid [bytes]
  (let [b-array (if (instance? js/Uint8Array bytes)
                  (create-array-from-typed bytes)
                  bytes)]
    (UUID. (string/join "-" (uuid/unparse b-array)) nil)))

(defn read-utf8-chars [source offset length]
  (let [buf (js/Array.)]
    (loop [pos 0]
      (let [ch (bit-and (aget source pos) 0xff)
            ch>>4 (bit-shift-right ch 4)]
        (when (< pos length)
          (cond
           (<=  0 ch>>4 7) (do (.push buf ch) (recur (inc pos)))
           (<= 12 ch>>4 13) (let [ch1 (aget source (inc pos))]
                           (.push buf (bit-or
                                       (bit-shift-left
                                        (bit-and ch 0x1f) 6)
                                       (bit-and ch1 0x3f)))
                           (recur (+ pos 2)))
           (= ch>>4 14) (let [ch1 (aget source (inc pos))
                           ch2 (aget source (+ pos 2))]
                       (.push buf (bit-or
                                   (bit-shift-left
                                    (bit-and ch 0x0f) 12)
                                   (bit-shift-left
                                    (bit-and ch1 0x03f) 6)
                                   (bit-and ch2 0x3f)))
                       (recur (+ pos 3)))
           :default (throw (gstring/format "Invalid UTF-8: %d" ch))))))
    (.apply (.-fromCharCode js/String) nil buf)))

(defn utf8-encoding-size [ch]
  (cond
    (<= ch 0x007F) 1
    (>  ch 0x07FF) 2
    :default 3))

(defn buffer-string-chunk-utf8 [s start buf]
  (loop [ string-pos start
          buffer-pos 0]
    (if (< string-pos (. s -length))
      (let [ ch (.charCodeAt s string-pos)
             encoding-size (utf8-encoding-size ch)]
        (if (<= (+ buffer-pos encoding-size) (. buf -length))
          (do
            (case encoding-size
              1 (aset buf buffer-pos ch)
              2 (do
                  (aset buf buffer-pos
                    (bit-or 0xC0 (bit-and (bit-shift-right ch 6) 0x1F)))
                  (aset buf (inc buffer-pos)
                    (bit-or 0x80 (bit-and (bit-shift-right ch 0) 0x3F))))
              3 (do
                  (aset buf buffer-pos
                    (bit-or 0xE0 (bit-and (bit-shift-right ch 12) 0x0F)))
                  (aset buf (inc buffer-pos)
                    (bit-or 0x80 (bit-and (bit-shift-right ch 6) 0x3F)))
                  (aset buf (+ buffer-pos 2)
                    (bit-or 0x80 (bit-and (bit-shift-right ch 0) 0x3F)))))
            (recur (inc string-pos) (+ buffer-pos encoding-size)))
          [string-pos buffer-pos]))
      [string-pos buffer-pos])))


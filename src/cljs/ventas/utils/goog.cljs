(ns ventas.utils.goog
  (:require
   [goog.string :as gstring]
   [goog.string.format]))

(defn format
  [fmt & args]
  (apply gstring/format fmt args))
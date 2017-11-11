(ns ventas.utils.goog
  (:require
   [goog.string :as gstring]
   [goog.string.format]))

(defn format
  [fmt & args]
  (js/console.log "goog format" fmt args)
  (apply gstring/format fmt args))
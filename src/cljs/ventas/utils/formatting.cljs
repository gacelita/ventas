(ns ventas.utils.formatting
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [ventas.i18n :refer [i18n]]))

(defn goog-format
  [fmt & args]
  (apply gstring/format fmt args))

(defn format-number [n unit]
  (str
   (goog-format (str "%.2f") n)
   " "
   (i18n unit)))
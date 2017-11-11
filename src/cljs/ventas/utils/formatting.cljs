(ns ventas.utils.formatting
  (:require
   [ventas.utils.goog :as utils.goog]
   [ventas.i18n :refer [i18n]]))

(defn format-number [n unit]
  (str
   (utils.goog/format (str "%.2f") n)
   " "
   (i18n unit)))
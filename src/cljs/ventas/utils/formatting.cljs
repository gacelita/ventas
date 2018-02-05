(ns ventas.utils.formatting
  (:require
   [ventas.utils.goog :as utils.goog]
   [ventas.i18n :refer [i18n]]))

(defn format-number [n]
  (when n
    (utils.goog/format (str "%.2f") n)))

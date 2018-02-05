(ns ventas.utils.formatting
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.utils.goog :as utils.goog]))

(defn format-number [n]
  (when n
    (utils.goog/format (str "%.2f") n)))

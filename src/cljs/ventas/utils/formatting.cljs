(ns ventas.utils.formatting
  (:require
   [ventas.common.utils :as common.utils]
   [ventas.i18n :refer [i18n]]
   [ventas.utils.goog :as utils.goog]))

(defn format-number [n]
  (when n
    (utils.goog/format (str "%.2f") n)))

(defn amount->str [{:keys [value currency]}]
  (str (format-number (common.utils/bigdec->str value))
       " "
       (:symbol currency)))

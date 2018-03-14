(ns ventas.utils.formatting
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.utils.goog :as utils.goog]
   [ventas.common.utils :as common.utils]))

(defn format-number [n]
  (when n
    (utils.goog/format (str "%.2f") n)))

(defn amount->str [{:keys [value currency]}]
  (str (format-number (common.utils/bigdec->str value))
       " "
       (:symbol currency)))
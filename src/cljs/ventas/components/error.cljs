(ns ventas.components.error
  (:require
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

(defn no-data []
  [:div.error.error--no-data
   [base/icon {:name "warning sign"}]
   [:p (i18n ::no-data)]])

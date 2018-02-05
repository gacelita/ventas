(ns ventas.components.error
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.components.base :as base]))

(defn no-data []
  [:div.error.error--no-data
   [base/icon {:name "warning sign"}]
   [:p (i18n ::no-data)]])

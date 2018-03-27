(ns ventas.components.error
  (:require
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

(defn no-data [& {:keys [message]}]
  [:div.error-component.error--no-data
   [base/icon {:name "warning sign"}]
   [:p (or message (i18n ::no-data))]])

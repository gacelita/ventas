(ns ventas.pages.admin.payment-methods
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.pages.admin.configuration :as admin.configuration]
   [ventas.routes :as routes]
   [re-frame.core :as rf]))

(defn get-items []
  (->> @(rf/subscribe [::admin.skeleton/menu-items])
       (filter #(= (:route %) :admin.payment-methods))
       first
       :children))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-payment-methods__page
    [admin.configuration/menu (get-items)]]])

(routes/define-route!
  :admin.payment-methods
  {:name ::page
   :url "payment-methods"
   :component page})

(ns ventas.pages.admin.payment-methods
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-payment-methods__page
    [:p "Nothing here!"]]])

(routes/define-route!
  :admin.payment-methods
  {:name ::page
   :url "payment-methods"
   :component page})

(ns ventas.pages.admin
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.configuration]
   [ventas.pages.admin.dashboard]
   [ventas.pages.admin.plugins]
   [ventas.pages.admin.products.edit]
   [ventas.pages.admin.products]
   [ventas.pages.admin.skeleton :as skeleton]
   [ventas.pages.admin.orders]
   [ventas.pages.admin.orders.edit]
   [ventas.pages.admin.users]
   [ventas.pages.admin.users.edit]
   [ventas.pages.admin.taxes]
   [ventas.pages.admin.payment-methods]
   [ventas.pages.admin.activity-log]
   [ventas.routes :as routes]))

(defn page []
  [skeleton/skeleton
   [:p.admin__default-content
    (i18n ::nothing-here)]])

(routes/define-route!
  :admin
  {:name ::page
   :url "admin"
   :component page})

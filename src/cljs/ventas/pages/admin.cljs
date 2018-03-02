(ns ventas.pages.admin
  (:require
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.activity-log]
   [ventas.pages.admin.configuration]
   [ventas.pages.admin.configuration.image-sizes]
   [ventas.pages.admin.configuration.email]
   [ventas.pages.admin.dashboard :as dashboard]
   [ventas.pages.admin.orders.edit]
   [ventas.pages.admin.orders]
   [ventas.pages.admin.payment-methods]
   [ventas.pages.admin.plugins]
   [ventas.pages.admin.products.edit]
   [ventas.pages.admin.products]
   [ventas.pages.admin.products.discounts]
   [ventas.pages.admin.products.discounts.edit]
   [ventas.pages.admin.taxes]
   [ventas.pages.admin.users.edit]
   [ventas.pages.admin.users]
   [ventas.routes :as routes]))

(routes/define-route!
  :admin
  {:name ::dashboard/page
   :url "admin"
   :component dashboard/page
   :init-fx [::dashboard/init]})
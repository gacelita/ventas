(ns ventas.pages.admin
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.activity-log]
   [ventas.pages.admin.configuration]
   [ventas.pages.admin.configuration.email]
   [ventas.pages.admin.configuration.image-sizes]
   [ventas.pages.admin.dashboard :as dashboard]
   [ventas.pages.admin.orders]
   [ventas.pages.admin.orders.edit]
   [ventas.pages.admin.payment-methods]
   [ventas.pages.admin.plugins]
   [ventas.pages.admin.products]
   [ventas.pages.admin.products.discounts]
   [ventas.pages.admin.products.discounts.edit]
   [ventas.pages.admin.products.edit]
   [ventas.pages.admin.shipping-methods]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.pages.admin.taxes]
   [ventas.pages.admin.users]
   [ventas.pages.admin.users.edit]
   [ventas.routes :as routes]))

(rf/reg-event-fx
 ::handle-route-change
 (fn [{:keys [db]} [_ [_ handler]]]
   (if (and (str/starts-with? (name handler) "admin")
            (not (get-in db [::state :init-done?])))
     {:dispatch [::admin.skeleton/init]
      :db (assoc-in db [::state :init-done?] true)}
     {})))

(rf/reg-event-fx
 ::listen-to-route-change
 (fn [_ _]
   {:forward-events {:register ::route-listener
                     :events #{::routes/set}
                     :dispatch-to [::handle-route-change]}}))

(rf/dispatch [::listen-to-route-change])

(routes/define-route!
  :admin
  {:name ::dashboard/page
   :url "admin"
   :component dashboard/page
   :init-fx [::dashboard/init]})

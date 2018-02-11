(ns ventas.pages.admin.products.discounts.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} [_ data]]
   {:dispatch [::backend/admin.orders.save
               {:params (get-in db [state-key :order])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::order-saved-notification)
                                  :theme "success"}]
    :go-to [:admin.orders]}))

(rf/reg-event-fx
 ::fetch
 (fn [cofx [_ id]]
   {:dispatch [::backend/admin.entities.pull
               {:params {:id id}
                :success [::form/populate state-key]}]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::fetch (routes/ref-from-param :id)]
                 [::events/enums.get :discount.amount.kind]]}))

(defn- content []
  (let [{:keys [order]} @(rf/subscribe [::events/db state-key])]

    [base/form {:key (:db/id order)
                :on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     [base/segment {:color "orange"
                    :title (i18n ::order)}]

     [base/form-button {:type "submit"} (i18n ::submit)]]))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-discounts-edit__page]])

(routes/define-route!
 :admin.products.discounts.edit
 {:name ::page
  :url [:id "/edit"]
  :component page})
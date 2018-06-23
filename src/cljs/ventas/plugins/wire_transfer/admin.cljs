(ns ventas.plugins.wire-transfer.admin
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(admin.skeleton/add-menu-item!
 {:route :admin.payment-methods.wire-transfer
  :label ::page
  :parent :admin.payment-methods})

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.configuration.set
               {:params (get-in db [state-key :form])
                :success [::notificator/notify-saved]}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(defn- content []
  [form/form [state-key]
   [base/segment {:color "orange"
                  :title (i18n ::page)}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     [field {:key :wire-transfer.account-details}]
     [field {:key :wire-transfer.account-owner}]
     [field {:key :wire-transfer.bank-address}]

     [base/divider {:hidden true}]

     [base/form-button {:type "submit"}
      (i18n ::submit)]]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-payment-methods-wire-transfer__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/configuration.get
                  {:params #{:wire-transfer.account-details
                             :wire-transfer.account-owner
                             :wire-transfer.bank-address}
                   :success [::form/populate [state-key]]}]]}))

(routes/define-route!
 :admin.payment-methods.wire-transfer
 {:name ::page
  :url "wire-transfer"
  :component page
  :init-fx [::init]})

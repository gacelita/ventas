(ns ventas.plugins.wire-transfer.frontend
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.payment :as payment]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::backend/configuration.get
               {:params #{:wire-transfer.account-owner
                          :wire-transfer.account-details
                          :wire-transfer.bank-address}
                :success ::init.next}]}))

(rf/reg-event-db
 ::init.next
 (fn [db [_ data]]
   (assoc db state-key data)))

(defn wire-transfer []
  (let [data @(rf/subscribe [::events/db [state-key]])]
    [:div.wire-transfer
     [:p (i18n ::wire-transfer.account-owner) ": " (:wire-transfer.account-owner data)]
     [:p (i18n ::wire-transfer.account-details) ": " (:wire-transfer.account-details data)]
     [:p (i18n ::wire-transfer.bank-address) ": " (:wire-transfer.bank-address data)]]))

(payment/add-method
 :wire-transfer
 {:component wire-transfer
  :init-fx [::init]})

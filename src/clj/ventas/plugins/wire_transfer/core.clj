(ns ventas.plugins.wire-transfer.core
  "Adds a wire transfer payment method"
  (:require
   [ventas.database.entity :as entity]
   [ventas.entities.configuration :as configuration]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.payment-method :as payment-method]))

(defn- pay!
  "Just sets the order as unpaid.
   When the wire transfer is received, the administrator should change the order
   status (to :paid or :acknowledged)"
  [order _]
  (entity/update* (assoc order :order/status :order.status/unpaid))
  true)

(payment-method/register!
 :wire-transfer
 {:name (entities.i18n/get-i18n-entity {:en_US "Wire transfer"
                                        :es_ES "Transferencia bancaria"})
  :pay-fn pay!
  :init (fn []
          (configuration/register-key! :wire-transfer.account-owner)
          (configuration/register-key! :wire-transfer.account-details)
          (configuration/register-key! :wire-transfer.bank-address))})

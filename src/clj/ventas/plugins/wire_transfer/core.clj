(ns ventas.plugins.wire-transfer.core
  "Adds a wire transfer payment method"
  (:require
   [ventas.plugin :as plugin]
   [ventas.entities.configuration :as configuration]))

(plugin/register!
 :wire-transfer
 {:name "Wire transfer"
  :init (fn []
          (configuration/register-key! :wire-transfer.account-owner)
          (configuration/register-key! :wire-transfer.account-details)
          (configuration/register-key! :wire-transfer.bank-address))})
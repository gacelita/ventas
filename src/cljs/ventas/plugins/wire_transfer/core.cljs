(ns ventas.plugins.wire-transfer.core
  (:require
   [ventas.i18n :as i18n]
   [ventas.plugins.wire-transfer.admin :as admin]
   [ventas.plugins.wire-transfer.frontend :as frontend]))

(i18n/register-translations!
 {:en_US
  {::admin/page "Wire transfer"
   ::admin/wire-transfer.account-details "Account details"
   ::admin/wire-transfer.account-owner "Account owner"
   ::admin/wire-transfer.bank-address "Bank address"
   ::admin/submit "Submit"
   ::frontend/wire-transfer "Wire transfer"
   ::frontend/wire-transfer.account-owner "Account owner"
   ::frontend/wire-transfer.account-details "Account details"
   ::frontend/wire-transfer.bank-address "Bank address"}})

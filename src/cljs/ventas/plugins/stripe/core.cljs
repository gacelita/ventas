(ns ventas.plugins.stripe.core
  "Stripe configuration"
  (:require
   [ventas.i18n :as i18n :refer [i18n]]
   [ventas.plugins.stripe.admin :as admin]
   [ventas.plugins.stripe.frontend :as frontend]))

(i18n/register-translations!
 {:en_US
  {::admin/page "Stripe"
   ::admin/publishable-key "Public key"
   ::admin/private-key "Private key"
   ::admin/submit "Submit"
   ::frontend/pay-with-stripe "Pay with Stripe"}})

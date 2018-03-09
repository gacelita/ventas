(ns ventas.plugins.stripe.core
  "Adds a payment method for Stripe"
  (:require
   [ventas.plugin :as plugin]
   [taoensso.timbre :as timbre]
   [clj-stripe.common :as stripe]
   [clj-stripe.charges :as stripe.charges]
   [ventas.entities.configuration :as entities.configuration]))

(defn- handle-payment [data]
  (timbre/debug "Stripe payment" data))

(plugin/register!
 :stripe
 {:name "Stripe"
  :http-handler handle-payment})

(comment
 (stripe/with-token (entities.configuration/get :stripe.private-key)
                    (stripe/execute
                     (stripe.charges/create-charge
                      (stripe/money-quantity 5000 "usd")))))
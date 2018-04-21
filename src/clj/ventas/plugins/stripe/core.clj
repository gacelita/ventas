(ns ventas.plugins.stripe.core
  "Adds a payment method for Stripe"
  (:require
   [clj-stripe.charges :as stripe.charges]
   [clj-stripe.common :as stripe]
   [taoensso.timbre :as timbre]
   [ventas.entities.configuration :as configuration]
   [ventas.payment-method :as payment-method]))

(defn- pay! [order params]
  (timbre/debug {:stripe-pay! params})
  true)

(defn- handle-payment [data]
  (timbre/debug {:stripe-http-handler data}))

(payment-method/register!
 :stripe
 {:name "Stripe"
  :http-handler handle-payment
  :pay-fn pay!
  :init (fn []
          (configuration/register-key! :stripe.private-key #{:user.role/administrator})
          (configuration/register-key! :stripe.public-key))})

(comment
 (stripe/with-token (configuration/get :stripe.private-key)
                    (stripe/execute
                     (stripe.charges/create-charge
                      (stripe/money-quantity 5000 "usd")))))

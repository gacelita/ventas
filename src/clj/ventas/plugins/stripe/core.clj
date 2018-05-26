(ns ventas.plugins.stripe.core
  "Adds a payment method for Stripe"
  (:require
   [clj-stripe.charges :as stripe.charges]
   [clj-stripe.common :as stripe]
   [taoensso.timbre :as timbre]
   [ventas.entities.configuration :as configuration]
   [ventas.payment-method :as payment-method]
   [ventas.server.api :as api]
   [ventas.entities.order :as entities.order]
   [ventas.database.entity :as entity]))

(defn- pay! [order {:keys [token]}]
  (let [{:amount/keys [currency value]} (entities.order/get-amount order)
        {:currency/keys [keyword] :as amount} (entity/find currency)
        {:keys [id error]} (stripe/with-token
                            (configuration/get :stripe.private-key)
                            (stripe/execute
                             (stripe.charges/create-charge
                              (stripe/money-quantity (* 100 value) (name keyword))
                              (stripe/card token))))]
    (if error
      (throw (Exception. (:message error)))
      (do
        (entity/update* (assoc order :order/payment-reference id
                                     :order/payment-amount amount
                                     :order/status :order.status/paid))
        true))))

(payment-method/register!
 :stripe
 {:name "Stripe"
  :pay-fn pay!
  :init (fn []
          (configuration/register-key! :stripe.private-key #{:user.role/administrator})
          (configuration/register-key! :stripe.public-key))})
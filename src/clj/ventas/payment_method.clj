(ns ventas.payment-method
  "The API for registering payment methods.
   Payment methods are responsible for:
     - Transforming an :order entity into the shape they need
     - Making the HTTP requests they need and handling them correctly
     - Updating the :order with the resulting :payment-reference and :payment-amount
     - Changing the status of the :order from :draft to :paid or :unpaid
   If an error occurs, it should be registered in the event log, and the order should be
   left unmodified.

   To allow the payment methods to do their work, they can do anything they need, but most likely
   they'll want to:
     - Register Ring handlers: use ventas.plugin's :http-handler
     - Make HTTP requests: `clj-http` is included as a dependency"
  (:require
   [slingshot.slingshot :refer [throw+]]
   [ventas.database.entity :as entity]
   [ventas.plugin :as plugin]))

(defn register! [kw attrs]
  (plugin/register! kw (merge attrs
                              {:type :payment-method})))

(defn all []
  (plugin/by-type :payment-method))

(defn pay!
  "Launches a payment method for an order"
  [order params]
  {:pre [(:order/payment-method order) (entity/entity? order)]}
  (let [method (plugin/find (:order/payment-method order))]
    (when-not method
      (throw+ {:type ::payment-method-not-found
               :method method}))
    ((:pay-fn method) order params)))

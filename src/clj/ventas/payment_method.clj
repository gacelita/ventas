(ns ventas.payment-method
  "The API for registering payment methods.
   Payment methods are responsible for:
     - Transforming an :order entity into the form they need
     - Making the HTTP requests they need and handling them correctly
     - Updating the :order with the resulting :payment-reference and :payment-amount
     - Changing the status of the :order from :unpaid to :paid if nothing went wrong
   If an error occurs, it should be registered in the event log, and the order should be
   left unmodified.

   To allow the payment methods to do their work, they can do anything they need, but most likely
   they'll want to:
     - Register Ring handlers: use ventas.plugin's :http-handler
     - Make HTTP requests: `clj-http` is included as a dependency"
  (:require
   [ventas.utils :as utils]
   [clojure.spec.alpha :as spec]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.database.entity :as entity]))

;; Used only in the backoffice
(spec/def ::name ::entities.i18n/ref)

;; Will be called with the order to be paid
(spec/def ::pay-fn fn?)

(spec/def ::attrs
  (spec/keys :req-un [::name
                      ::pay-fn]))

(defonce payment-methods (atom {}))

(defn register! [kw attrs]
  {:pre [(keyword? kw) (utils/check ::attrs attrs)]}
  (swap! payment-methods assoc kw attrs))

(defn payment-method [kw]
  {:pre [(keyword? kw)]}
  (get @payment-methods kw))

(defn pay!
  "Launches a payment method for an order"
  [order]
  {:pre [(:order/payment-method order) (entity/entity? order)]}
  (let [method (payment-method (:order/payment-method order))]
    (when-not method
      (throw (Exception. (str "The " method " payment method does not exist"))))
    ((:pay-fn method) order)))
(ns ventas.entities.order
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

#_"
  Orders have:
  - An user
  - A list of products with their variations and quantities
  - A status
  - Billing and shipping addresses
  - A shipping method (maybe with comments)
  - A payment method
"

(spec/def :order/user
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :user)))))

(spec/def :order/status #{:order.status/unpaid
                       :order.status/paid
                       :order.status/acknowledged
                       :order.status/ready
                       :order.status/shipped})

(spec/def :order/shipping-address
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :address)))))

(spec/def :order/billing-address
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :address)))))

(spec/def :order/shipping-method keyword?)
(spec/def :order/shipping-comments string?)

(spec/def :order/payment-method keyword?)
(spec/def :order/payment-reference string?)

(spec/def :schema.type/order
  (spec/keys :req [:order/user
                :order/status
                :order/shipping-address
                :order/billing-address
                :order/shipping-method
                :order/payment-method]
          :opt [:order/shipping-comments
                :order/payment-reference]))

(entity/register-type! :order)
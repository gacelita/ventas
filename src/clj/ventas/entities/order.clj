(ns ventas.entities.order
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]))

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
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :user)))))

(spec/def :order/status #{:order.status/unpaid
                       :order.status/paid
                       :order.status/acknowledged
                       :order.status/ready
                       :order.status/shipped})

(spec/def :order/shipping-address
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :address)))))

(spec/def :order/billing-address
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :address)))))

(spec/def :order/shipping-method ::generators/keyword)
(spec/def :order/shipping-comments ::generators/string)

(spec/def :order/payment-method ::generators/keyword)
(spec/def :order/payment-reference ::generators/string)

(spec/def :schema.type/order
  (spec/keys :req [:order/user
                   :order/status
                   :order/shipping-address
                   :order/billing-address
                   :order/shipping-method
                   :order/payment-method]
             :opt [:order/shipping-comments
                   :order/payment-reference]))

(entity/register-type!
 :order
 {:attributes
  [{:db/ident :order/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.status/unpaid}
   {:db/ident :order.status/paid}
   {:db/ident :order.status/acknowledged}
   {:db/ident :order.status/ready}
   {:db/ident :order.status/shipped}

   {:db/ident :order/shipping-address
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/billing-address
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/shipping-method
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/payment-method
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/shipping-comments
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/payment-reference
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:address :user}})
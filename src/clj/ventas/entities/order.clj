(ns ventas.entities.order
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]))

(spec/def :order/user
  (spec/with-gen ::entity/ref #(entity/ref-generator :user)))

(spec/def :order/status
  #{:order.status/unpaid
    :order.status/paid
    :order.status/acknowledged
    :order.status/ready
    :order.status/shipped
    :order.status/draft})

(spec/def :order/shipping-address
  (spec/with-gen ::entity/ref #(entity/ref-generator :address)))

(spec/def :order/billing-address
  (spec/with-gen ::entity/ref #(entity/ref-generator :address)))

(spec/def :order/shipping-method ::generators/keyword)

(spec/def :order/shipping-comments ::generators/string)

(spec/def :order/payment-method ::generators/keyword)

(spec/def :order/payment-reference ::generators/string)

(spec/def :order/payment-amount
  (spec/with-gen ::entity/ref #(entity/ref-generator :amount)))

(spec/def :order/lines
  (spec/with-gen ::entity/refs #(entity/refs-generator :order.line)))

(spec/def :schema.type/order
  (spec/keys :req [:order/user
                   :order/status]
             :opt [:order/shipping-comments
                   :order/payment-reference
                   :order/shipping-address
                   :order/billing-address
                   :order/shipping-method
                   :order/payment-method
                   :order/lines]))

(entity/register-type!
 :order
 {:attributes
  [{:db/ident :order/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.status/draft}
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

   {:db/ident :order/lines
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident :order/payment-reference
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :order/payment-amount
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}]

  :dependencies
  #{:address :user :amount}})

(spec/def :order.line/product-variation
  (spec/with-gen ::entity/ref #(entity/ref-generator :product.variation)))

(spec/def :order.line/quantity (spec/and integer? pos?))

(spec/def :schema.type/order.line
  (spec/keys :req [:order.line/product-variation
                   :order.line/quantity]))

(entity/register-type!
 :order.line
 {:attributes
  [{:db/ident :order.line/product-variation
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/quantity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :order.line/discount
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :autoresolve? true

  :dependencies
  #{:order :product :product.variation :discount}})
(ns ventas.entities.order-line
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]))

(spec/def :order-line/order
  (spec/with-gen ::entity/ref #(entity/ref-generator :order)))

(spec/def :order-line/product-variation
  (spec/with-gen ::entity/ref #(entity/ref-generator :product.variation)))

(spec/def :order-line/quantity (spec/and integer? pos?))

(spec/def :schema.type/order-line
  (spec/keys :req [:order-line/order
                   :order-line/product-variation
                   :order-line/quantity]))

(entity/register-type!
 :order-line
 {:attributes
  [{:db/ident :order-line/order
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order-line/product-variation
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order-line/quantity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:order :product :product.variation}})
(ns ventas.entities.order-line
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]))

(spec/def :order-line/order
  (spec/with-gen ::entity/ref #(entity/ref-generator :order)))

(spec/def :order-line/product
  (spec/with-gen ::entity/ref #(entity/ref-generator :product)))

(spec/def :order-line/terms
  (spec/with-gen ::entity/refs #(entity/refs-generator :product.term)))

(spec/def :order-line/quantity (spec/and integer? pos?))

(spec/def :schema.type/order-line
  (spec/keys :opt [:order-line/terms]
             :req [:order-line/order
                   :order-line/product
                   :order-line/quantity]))

(entity/register-type!
 :order-line
 {:attributes
  [{:db/ident :order-line/order
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order-line/product
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :order-line/quantity
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :order-line/terms
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]

  :dependencies
  #{:order :product :product.term}})
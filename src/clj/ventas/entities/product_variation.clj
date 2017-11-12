(ns ventas.entities.product-variation
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]))

(spec/def :product.variation/product
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :product)))

(spec/def :product.variation/terms
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :product.taxonomy.term)))

(spec/def :schema.type/product.variation
  (spec/keys :req [:product.variation/product
                   :product.variation/terms]))

(entity/register-type!
 :product.variation
 {:attributes
  [{:db/ident :product.variation/product
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product.variation/terms
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]

  :dependencies
  #{:product :product.taxonomy.term}})
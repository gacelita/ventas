(ns ventas.entities.product-term
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :product.term/name ::entities.i18n/ref)

(spec/def  :product.term/taxonomy
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :product.taxonomy)))

(spec/def :schema.type/product.term
  (spec/keys :req [:product.term/name
                   :product.term/taxonomy]))

(entity/register-type!
 :product.term
 {:attributes
  [{:db/ident :product.term/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product.term/taxonomy
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:product.taxonomy :i18n}})
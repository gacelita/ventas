(ns ventas.entities.product-taxonomy-term
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :product.taxonomy.term/name ::entities.i18n/ref)

(spec/def  :product.taxonomy.term/taxonomy
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :product.taxonomy)))

(spec/def :schema.type/product.taxonomy.term
  (spec/keys :req [:product.taxonomy.term/name
                   :product.taxonomy.term/taxonomy]))

(entity/register-type!
 :product.taxonomy.term
 {:attributes
  [{:db/ident :product.taxonomy.term/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product.taxonomy.term/taxonomy
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:product.taxonomy :i18n}})
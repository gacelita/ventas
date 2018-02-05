(ns ventas.entities.amount
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]))

(spec/def :amount/keyword ::generators/keyword)

(spec/def :amount/value ::generators/bigdec)

(spec/def :amount/currency
  (spec/with-gen ::entity/ref #(entity/ref-generator :currency)))

(spec/def :schema.type/amount
  (spec/keys :req [:amount/keyword
                   :amount/value
                   :amount/currency]))

(entity/register-type!
 :amount
 {:attributes
  [{:db/ident :amount/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :amount/value
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/index true}
   {:db/ident :amount/currency
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:currency}

  :seed-number 0
  :autoresolve? true
  :component? true})

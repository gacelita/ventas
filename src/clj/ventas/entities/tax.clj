(ns ventas.entities.tax
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :tax/name string?)
(spec/def :tax/kind #{:tax.kind/percentage :tax.kind/amount})
(spec/def :tax/amount double?)

(spec/def :schema.type/tax
  (spec/keys :req [:tax/name :tax/kind :tax/amount]))

(entity/register-type!
 :tax
 {:attributes
  [{:db/ident :tax/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :tax/amount
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one}

   {:db/ident :tax/kind
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :tax.kind/percentage}
   {:db/ident :tax.kind/amount}]})
(ns ventas.entities.tax
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :tax/name ::entities.i18n/ref)

(spec/def :tax/kind #{:tax.kind/percentage :tax.kind/amount})

(spec/def :tax/amount double?)

(spec/def :tax/keyword ::generators/keyword)

(spec/def :schema.type/tax
  (spec/keys :req [:tax/name
                   :tax/kind
                   :tax/amount
                   :tax/keyword]))

(entity/register-type!
 :tax
 {:attributes
  [{:db/ident :tax/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :ventas/refEntityType :i18n}

   {:db/ident :tax/keyword
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :tax/amount
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one}

   {:db/ident :tax/kind
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :tax.kind/percentage}
   {:db/ident :tax.kind/amount}]

  :autoresolve? true

  :dependencies
  #{:i18n}})
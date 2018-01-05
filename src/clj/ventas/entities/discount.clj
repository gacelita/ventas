(ns ventas.entities.discount
  (:require
   [ventas.database.entity :as entity]
   [clojure.spec.alpha :as spec]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :discount/active? boolean?)

(spec/def :discount/code ::generators/string)

(spec/def :discount/name ::entities.i18n/ref)

(spec/def :discount/action
  (spec/with-gen ::entity/ref #(entity/ref-generator :discount.action)))

(spec/def :schema.type/discount
  (spec/keys :req [:discount/name
                   :discount/code
                   :discount/active?
                   :discount/max-uses-per-customer
                   :discount/max-uses
                   :discount/action]))

(entity/register-type!
 :discount
 {:attributes
  [{:db/ident :discount/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/ident :discount/code
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}

   {:db/ident :discount/max-uses
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount/max-uses-per-customer
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount/active?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount/action
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}]

  :autoresolve? true

  :dependencies
  #{:i18n :discount.action}})


(spec/def :discount.action/free-shipping? boolean?)

(spec/def :discount.action/product
  (spec/with-gen ::entity/ref #(entity/ref-generator :product)))

(spec/def :discount.action/amount
  (spec/with-gen ::entity/ref #(entity/ref-generator :amount)))

(spec/def :discount.action/amount.tax-included? boolean?)

(spec/def :discount.action/amount.kind #{})


(spec/def :schema.type/discount.action
  (spec/keys :opt [:discount.action/free-shipping?
                   :discount.action/product
                   :discount.action/amount
                   :discount.action/amount.tax-included?
                   :discount.action/amount.kind]))

(entity/register-type!
 :discount.action
 {:attributes
  [{:db/ident :discount.action/free-shipping?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount.action/product
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount.action/amount
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/ident :discount.action/amount.tax-included?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount.action/amount.kind
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :discount.action.amount.kind/percentage}
   {:db/ident :discount.action.amount.kind/amount}]

  :autoresolve? true

  :dependencies
  #{:product :amount}})

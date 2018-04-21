(ns ventas.entities.discount
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :discount/active? boolean?)

(spec/def :discount/code ::generators/string)

(spec/def :discount/name ::entities.i18n/ref)

(spec/def :discount/free-shipping? boolean?)

(spec/def :discount/product
  (spec/with-gen ::entity/ref #(entity/ref-generator :product)))

(spec/def :discount/amount
  (spec/with-gen ::entity/ref #(entity/ref-generator :amount)))

(spec/def :discount/amount.tax-included? boolean?)

(def amount-kinds
  #{:discount.amount.kind/percentage
    :discount.amount.kind/amount})

(spec/def :discount/amount.kind
  (spec/with-gen
   (spec/or :pull-eid ::db/pull-eid
            :kind amount-kinds)
   #(gen/elements amount-kinds)))

(spec/def :schema.type/discount
  (spec/keys :opt [:discount/name
                   :discount/code
                   :discount/active?
                   :discount/max-uses-per-customer
                   :discount/max-uses
                   :discount/free-shipping?
                   :discount/product
                   :discount/amount
                   :discount/amount.tax-included?
                   :discount/amount.kind]))

(entity/register-type!
 :discount
 {:attributes
  (concat
   [{:db/ident :discount/name
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/isComponent true
     :ventas/refEntityType :i18n}

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

    {:db/ident :discount/free-shipping?
     :db/valueType :db.type/boolean
     :db/cardinality :db.cardinality/one}

    {:db/ident :discount/product
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}

    {:db/ident :discount/amount
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/isComponent true
     :ventas/refEntityType :amount}

    {:db/ident :discount/amount.tax-included?
     :db/valueType :db.type/boolean
     :db/cardinality :db.cardinality/one}

    {:db/ident :discount/amount.kind
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}]

   (map #(hash-map :db/ident %) amount-kinds))

  :autoresolve? true

  :dependencies
  #{:i18n}})

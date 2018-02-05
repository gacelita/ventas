(ns ventas.entities.shipping-method
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :shipping-method.price/min-value ::generators/bigdec)

(spec/def :shipping-method.price/max-value ::generators/bigdec)

(spec/def :shipping-method.price/amount
  (spec/with-gen ::entity/ref #(entity/ref-generator :amount)))

(spec/def :shipping-method.price/country-groups
  (spec/with-gen ::entity/refs #(entity/refs-generator :country.group)))

(spec/def :schema.type/shipping-method.price
  (spec/keys :req [:shipping-method.price/min-value
                   :shipping-method.price/max-value
                   :shipping-method.price/amount
                   :shipping-method.price/country-groups]))

(entity/register-type!
 :shipping-method.price
 {:attributes
  [{:db/ident :shipping-method.price/min-value
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one}

   {:db/ident :shipping-method.price/max-value
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one}

   {:db/ident :shipping-method.price/amount
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/ident :shipping-method.price/country-groups
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]

  :dependencies
  #{:amount :country.group}

  :autoresolve? true
  :component? true})

(spec/def :shipping-method/name ::entities.i18n/ref)

(spec/def :shipping-method/default? boolean?)

(spec/def :shipping-method/manipulation-fee
  (spec/with-gen ::entity/ref #(entity/ref-generator :amount)))

(spec/def :shipping-method/pricing #{:shipping-method.pricing/price
                                     :shipping-method.pricing/weight})

(spec/def :shipping-method/prices
  (spec/with-gen ::entity/refs #(entity/refs-generator :shipping-method.price)))

(spec/def :schema.type/shipping-method
  (spec/keys :req [:shipping-method/name
                   :shipping-method/default?
                   :shipping-method/manipulation-fee
                   :shipping-method/pricing
                   :shipping-method/prices]))

(entity/register-type!
 :shipping-method
 {:attributes
  [{:db/ident :shipping-method/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :ventas/refEntityType :i18n}

   {:db/ident :shipping-method/default?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :shipping-method/manipulation-fee
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/ident :shipping-method/pricing
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :shipping-method.pricing/price}
   {:db/ident :shipping-method.pricing/weight}

   {:db/ident :shipping-method/prices
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}]

  :dependencies
  #{:i18n :amount :shipping-method.price}

  :autoresolve? true})

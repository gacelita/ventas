(ns ventas.entities.tax
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :as utils]))

(spec/def :tax/name ::entities.i18n/ref)

(def kinds
  #{:tax.kind/percentage
    :tax.kind/amount})

(spec/def :tax/kind
  (spec/with-gen
   (spec/or :pull-eid ::db/pull-eid
            :kind kinds)
   #(gen/elements kinds)))

(spec/def :tax/amount
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :amount)))

(spec/def :tax/keyword ::generators/keyword)

(spec/def :schema.type/tax
  (spec/keys :req [:tax/name
                   :tax/kind
                   :tax/amount]
             :opt [:tax/keyword]))

(entity/register-type!
 :tax
 {:migrations
  [[:base (utils/into-n
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
             :db/valueType :db.type/ref
             :db/cardinality :db.cardinality/one
             :db/isComponent true
             :ventas/refEntityType :amount}

            {:db/ident :tax/kind
             :db/valueType :db.type/ref
             :db/cardinality :db.cardinality/one
             :ventas/refEntityType :enum}]

           (map #(hash-map :db/ident %) kinds))]]

  :autoresolve? true

  :dependencies
  #{:i18n}})

(ns ventas.entities.amount
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils :as utils]
   [ventas.search.indexing :as search.indexing]))

(spec/def :amount/keyword ::generators/keyword)

(spec/def :amount/value ::generators/bigdec)

(spec/def :amount/currency
  (spec/with-gen ::entity/ref #(entity/ref-generator :currency)))

(spec/def :schema.type/amount
  (spec/keys :req [:amount/value
                   :amount/currency]
             :opt [:amount/keyword]))

(entity/register-type!
 :amount
 {:migrations
  [[:base [{:db/ident :amount/keyword
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}
           {:db/ident :amount/value
            :db/valueType :db.type/bigdec
            :db/cardinality :db.cardinality/one
            :db/index true}
           {:db/ident :amount/currency
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}]]]

  :dependencies
  #{:currency}

  :seed-number 0
  :autoresolve? true
  :component? true})

(defmethod search.indexing/transform-entity-by-type :schema.type/amount [entity]
  (:amount/value entity))

(defn ->entity
  "Creates an amount entity from the given parameters. Meant for quick creation
  of amount entities"
  [amount currency-kw]
  {:pre [(utils/bigdec? amount) (keyword? currency-kw)]}
  {:schema/type :schema.type/amount
   :amount/currency [:currency/keyword currency-kw]
   :amount/value amount})

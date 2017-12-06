(ns ventas.entities.currency
  (:require
   [ventas.database.entity :as entity]
   [clojure.spec.alpha :as spec]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :currency/name ::entities.i18n/ref)
(spec/def :currency/plural-name ::entities.i18n/ref)
(spec/def :currency/keyword ::generators/keyword)
(spec/def :currency/symbol ::generators/string)

(spec/def :schema.type/currency
  (spec/keys :req [:currency/name
                   :currency/plural-name
                   :currency/keyword
                   :currency/symbol]))

(entity/register-type!
 :currency
 {:attributes
  [{:db/ident :currency/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :currency/plural-name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :currency/symbol
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :currency/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}]

  :seed-number 0

  :autoresolve? true

  :dependencies
  #{:i18n}

  :fixtures
  (fn []
    [{:currency/name (entities.i18n/get-i18n-entity {:en_US "euro"
                                                     :es_ES "euro"})
      :currency/plural-name (entities.i18n/get-i18n-entity {:en_US "euros"
                                                            :es_ES "euros"})
      :currency/keyword :eur
      :currency/symbol "€"}
     {:currency/name (entities.i18n/get-i18n-entity {:en_US "dollar"
                                                     :es_ES "dólar"})
      :currency/plural-name (entities.i18n/get-i18n-entity {:en_US "dollars"
                                                            :es_ES "dólares"})
      :currency/keyword :usd
      :currency/symbol "$"}])})
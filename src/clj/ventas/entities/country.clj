(ns ventas.entities.country
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.database.entity :as entity]))

(spec/def :country/name ::entities.i18n/ref)

(spec/def :schema.type/country
  (spec/keys :req [:country/name]))

(entity/register-type!
 :country
 {:attributes
  [{:db/ident :country/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:i18n}

  :fixtures
  (fn []
    [{:schema/type :schema.type/country
      :country/name (entities.i18n/get-i18n-entity {:en "Spain"
                                                    :es "España"})}
     {:schema/type :schema.type/country
      :country/name (entities.i18n/get-i18n-entity {:en "United States"
                                                    :es "Estados Unidos"})}])})
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
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :ventas/refEntityType :i18n}]

  :dependencies
  #{:i18n}

  :autoresolve? true

  :fixtures
  (fn []
    [{:country/name (entities.i18n/get-i18n-entity {:en_US "Spain"
                                                    :es_ES "España"})}
     {:country/name (entities.i18n/get-i18n-entity {:en_US "United States"
                                                    :es_ES "Estados Unidos"})}])})

(spec/def :schema.type/country.group
  (spec/keys :req [:country.group/name]))

(entity/register-type!
 :country.group
 {:attributes
  [{:db/ident :country.group/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :country.group/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:i18n}

  :autoresolve? true

  :fixtures
  (fn []
    [{:country.group/name (entities.i18n/get-i18n-entity {:en_US "Europe"
                                                          :es_ES "Europa"})
      :country.group/keyword :europe}

     {:country.group/name (entities.i18n/get-i18n-entity {:en_US "North America"
                                                          :es_ES "Norteamérica"})
      :country.group/keyword :north-america}

     {:country.group/name (entities.i18n/get-i18n-entity {:en_US "Europe (non-EU)"
                                                          :es_ES "Europe (fuera de la UE)"})
      :country.group/keyword :europe-non-eu}])})
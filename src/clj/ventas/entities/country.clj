(ns ventas.entities.country
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]))

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
    :db/unique :db.unique/identity
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
    :ventas/refEntityType :i18n}
   {:db/ident :country/keyword
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :country/group
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :ventas/refEntityType :country.group}]

  :dependencies
  #{:i18n :country.group}

  :autoresolve? true

  :fixtures
  (fn []
    [{:country/keyword :es
      :country/group [:country.group/keyword :europe]
      :country/name (entities.i18n/get-i18n-entity {:en_US "Spain"
                                                    :es_ES "España"})}
     {:country/keyword :us
      :country/group [:country.group/keyword :north-america]
      :country/name (entities.i18n/get-i18n-entity {:en_US "United States"
                                                    :es_ES "Estados Unidos"})}])})

(ns ventas.entities.country
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :schema.type/country.group
  (spec/keys :req [:country.group/name]))

(entity/register-type!
 :country.group
 {:migrations
  [[:base [{:db/ident :country.group/name
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}
           {:db/ident :country.group/keyword
            :db/valueType :db.type/keyword
            :db/unique :db.unique/identity
            :db/cardinality :db.cardinality/one}]]
   [:add-missing-is-component [{:db/ident :country.group/name
                                :db/valueType :db.type/ref
                                :db/isComponent true
                                :db/cardinality :db.cardinality/one}]]]

  :dependencies
  #{:i18n}

  :autoresolve? true})

(spec/def :country/name ::entities.i18n/ref)

(spec/def :schema.type/country
  (spec/keys :req [:country/name]))

(entity/register-type!
 :country
 {:migrations
  [[:base [{:db/ident :country/name
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
            :ventas/refEntityType :country.group}]]]

  :dependencies
  #{:i18n :country.group}

  :autoresolve? true})

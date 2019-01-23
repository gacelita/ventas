(ns ventas.entities.state
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :state/name ::entities.i18n/ref)

(spec/def :schema.type/state
  (spec/keys :req [:state/name]))

(entity/register-type!
 :state
 {:migrations
  [[:base [{:db/ident :state/keyword
            :db/valueType :db.type/keyword
            :db/unique :db.unique/identity
            :db/cardinality :db.cardinality/one}

           {:db/ident :state/country
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :ventas/refEntityType :country}

           {:db/ident :state/parent
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :ventas/refEntityType :state}

           {:db/ident :state/name
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :db/isComponent true
            :ventas/refEntityType :i18n}]]]

  :autoresolve? true
  :dependencies #{:i18n :country}})

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
 {:attributes
  [{:db/ident :state/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :ventas/refEntityType :i18n}]

  :autoresolve? true
  :dependencies #{:i18n}

  :fixtures
  (fn []
    [{:state/name (entities.i18n/get-i18n-entity {:en_US "Madrid"
                                                  :es_ES "Madrid"})}
     {:state/name (entities.i18n/get-i18n-entity {:en_US "Barcelona"
                                                  :es_ES "Barcelona"})}
     {:state/name (entities.i18n/get-i18n-entity {:en_US "León"
                                                  :es_ES "León"})}])})

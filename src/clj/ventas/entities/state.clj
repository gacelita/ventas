(ns ventas.entities.state
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :state/name ::generators/string)

(spec/def :schema.type/state
  (spec/keys :req [:state/name]))

(entity/register-type!
 :state
 {:attributes
  [{:db/ident :state/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]

  :autoresolve? true

  :fixtures
  (fn []
    [{:state/name "Madrid"}
     {:state/name "Barcelona"}
     {:state/name "León"}])})
(ns ventas.entities.state
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]))

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

(ns ventas.entities.event
  (:require
    [clojure.spec.alpha :as spec]
    [ventas.database.entity :as entity]
    [ventas.database.generators :as generators]))

(spec/def :event/kind ::generators/keyword)

(spec/def :schema.type/event
  (spec/keys :req [:event/kind]))

(entity/register-type!
  :event
  {:attributes
   [{:db/ident :event/kind
     :db/valueType :db.type/keyword
     :db/cardinality :db.cardinality/one}]})
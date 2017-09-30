(ns ventas.entities.attribute
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :attribute/name string?)

(spec/def :schema.type/attribute
  (spec/keys :req [:attribute/name]))

(entity/register-type!
 :attribute
 {:attributes
  [{:db/ident :attribute/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]})
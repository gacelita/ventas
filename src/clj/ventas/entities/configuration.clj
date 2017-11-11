(ns ventas.entities.configuration
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :configuration/keyword keyword?)
(spec/def :configuration/value string?)

(spec/def :schema.type/configuration
  (spec/keys :req [:configuration/keyword
                   :configuration/value]))

(entity/register-type!
 :configuration
 {:attributes
  [{:db/ident :configuration/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident :configuration/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]})
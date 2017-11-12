(ns ventas.entities.attribute-value
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :attribute-value/name string?)

(spec/def :attribute-value/attribute
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :attribute)))))

(spec/def :schema.type/attribute-value
  (spec/keys :req [:attribute-value/name :attribute-value/attribute]))

(entity/register-type!
 :attribute-value
 {:attributes
  [{:db/ident :attribute-value/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :attribute-value/attribute
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:attribute}})
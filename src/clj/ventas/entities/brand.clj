(ns ventas.entities.brand
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :brand/name string?)
(spec/def :brand/description string?)
(spec/def :brand/logo
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :file)))))

(spec/def :schema.type/brand
  (spec/keys :req [:brand/name :brand/description :brand/logo]))

(entity/register-type!
 :brand
 {:attributes
  [{:db/ident :brand/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/logo
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/brand
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]})
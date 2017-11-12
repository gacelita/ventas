(ns ventas.entities.address
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :address/name string?)
(spec/def :address/person-name string?)
(spec/def :address/address string?)
(spec/def :address/zip integer?)
(spec/def :address/city string?)
(spec/def :address/country
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :country)))))
(spec/def :address/state
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :state)))))
(spec/def :address/user
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :user)))))
(spec/def :address/phone string?)
(spec/def :address/comments string?)


(spec/def :schema.type/address
  (spec/keys :req [:address/name
                   :address/person-name
                   :address/address
                   :address/zip
                   :address/city
                   :address/country
                   :address/state
                   :address/user]
             :opt [:address/phone
                   :address/comments]))

(entity/register-type!
 :address
 {:attributes
  [{:db/ident :address/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/person-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/address
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/zip
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/city
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/country
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/state
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/comments
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/phone
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:country :state :user}})
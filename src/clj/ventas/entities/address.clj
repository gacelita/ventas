(ns ventas.entities.address
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.database.generators :as generators]))

(spec/def :address/first-name ::generators/string)

(spec/def :address/last-name ::generators/string)

(spec/def :address/company ::generators/string)

(spec/def :address/address ::generators/string)

(spec/def :address/address-second-line ::generators/string)

(spec/def :address/zip ::generators/string)

(spec/def :address/city ::generators/string)

(spec/def :address/country
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :country)))

(spec/def :address/state
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :state)))

(spec/def :address/user
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :user)))

(spec/def :schema.type/address
  (spec/keys :req [:address/first-name
                   :address/last-name
                   :address/address
                   :address/zip
                   :address/city
                   :address/country
                   :address/state
                   :address/user]
             :opt [:address/company
                   :address/address-second-line]))

(entity/register-type!
 :address
 {:attributes
  [{:db/ident :address/first-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/last-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/company
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/address
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/address-second-line
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :address/zip
    :db/valueType :db.type/string
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

   {:db/ident :address/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:country :state :user :i18n}})
(ns ventas.entities.brand
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.util :refer [update-if-exists]]))

(spec/def :brand/name ::entities.i18n/ref)

(spec/def :brand/description string?)

(spec/def :brand/logo
  (spec/with-gen integer? #(entity/ref-generator :file)))

(spec/def :schema.type/brand
  (spec/keys :req [:brand/name :brand/description :brand/logo]))

(entity/register-type!
 :brand
 {:attributes
  [{:db/ident :brand/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/logo
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/brand
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:file :i18n}

  :to-json
  (fn [this]
    (-> this
        (update-if-exists :brand/logo (comp entity/to-json entity/find))
        ((:to-json entity/default-type))))})
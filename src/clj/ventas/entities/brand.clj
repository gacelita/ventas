(ns ventas.entities.brand
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :refer [update-if-exists]]
   [ventas.database.generators :as generators]))

(spec/def :brand/name ::entities.i18n/ref)

(spec/def :brand/description ::entities.i18n/ref)

(spec/def :brand/keyword ::generators/keyword)

(spec/def :brand/logo
  (spec/with-gen integer? #(entity/ref-generator :file)))

(spec/def :schema.type/brand
  (spec/keys :req [:brand/name
                   :brand/description
                   :brand/keyword]
             :opt [:brand/logo]))

(entity/register-type!
 :brand
 {:attributes
  [{:db/ident :brand/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/keyword
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/description
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :brand/logo
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :autoresolve? true

  :dependencies
  #{:file :i18n}

  :fixtures
  (fn []
    [{:brand/name (entities.i18n/get-i18n-entity {:en_US "Test brand"})
      :brand/keyword :test-brand
      :brand/description (entities.i18n/get-i18n-entity {:en_US "This is the description of the test brand"})
      :brand/logo (:db/id (first (entity/query :file)))}])})
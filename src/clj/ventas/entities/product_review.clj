(ns ventas.entities.product-review
  (:require
    [ventas.database.entity :as entity]
    [ventas.database.generators :as generators]
    [clojure.spec.alpha :as spec]))

(spec/def :product.review/title ::generators/string)
(spec/def :product.review/user ::generators/string)
(spec/def :product.review/content ::generators/string)
(spec/def :product.review/rating double?)

(spec/def :schema.type/product.review
  (spec/keys :req [:product.review/user
                   :product.review/product]
             :opt [:product.review/title
                   :product.review/content
                   :product.review/rating]))

(entity/register-type!
  :product.review
  {:migrations
   [[:base [{:db/ident :product.review/title
             :db/cardinality :db.cardinality/one
             :db/valueType :db.type/string}
            {:db/ident :product.review/content
             :db/cardinality :db.cardinality/one
             :db/valueType :db.type/string}
            {:db/ident :product.review/user
             :db/cardinality :db.cardinality/one
             :db/valueType :db.type/ref
             :ventas/refEntityType :user}
            {:db/ident :product.review/product
             :db/cardinality :db.cardinality/one
             :db/valueType :db.type/ref
             :ventas/refEntityType :product}
            {:db/ident :product.review/rating
             :db/cardinality :db.cardinality/one
             :db/valueType :db.type/float}]]]})


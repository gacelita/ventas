(ns ventas.entities.category
  (:require [clojure.spec.alpha :as spec]
            [clojure.core.async :refer [<! go-loop]]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.events :as events]))

(spec/def :category/name string?)
(spec/def :category/parent
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :category)))))

(spec/def :category/image
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :file)))))

(spec/def :schema.type/category
  (spec/keys :req [:category/name]
             :opt [:category/image :category/parent]))

(entity/register-type!
 :category
 {:attributes
  [{:db/ident :category/parent
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :category/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :category/image
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :category/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}]
  :fixtures
  (fn []
    [{:schema/type :schema.type/category
      :category/name "Default"}])})
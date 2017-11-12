(ns ventas.entities.category
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.core.async :refer [<! go-loop]]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.events :as events]
   [ventas.util :refer [update-if-exists]]))

(spec/def :category/name string?)
(spec/def :category/parent
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :category)))))

(spec/def :category/image
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :file)))))

(spec/def :category/keyword keyword?)

(spec/def :schema.type/category
  (spec/keys :req [:category/name
                   :category/keyword]
             :opt [:category/image
                   :category/parent]))

(defn get-image [entity]
  (if (:category/image entity)
    (-> (:category/image entity)
        (entity/find)
        (entity/to-json))
    (-> (db/filtered-query '([_ :product/categories ?category]
                             [_ :product/images ?id])
                           {'?category (:db/id entity)})
        (first)
        (first)
        (entity/find)
        (entity/to-json))))

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
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}]

  :fixtures
  (fn []
    [{:schema/type :schema.type/category
      :category/name "Default"
      :category/keyword :default}
     {:schema/type :schema.type/category
      :category/name "Sample category"
      :category/keyword :sample-category}
     {:schema/type :schema.type/category
      :category/name "Winter"
      :category/keyword :winter}])

  :dependencies
  #{:file}

  :to-json
  (fn [this]
    (-> this
        (assoc :category/image (get-image this))
        ((:to-json entity/default-type))))})
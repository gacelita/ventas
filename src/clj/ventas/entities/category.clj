(ns ventas.entities.category
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :refer [update-if-exists]]
   [ventas.database.generators :as generators]))

(spec/def :category/name ::entities.i18n/ref)

(spec/def :category/parent
  (spec/with-gen integer? #(entity/ref-generator :category)))

(spec/def :category/image
  (spec/with-gen integer? #(entity/ref-generator :file)))

(spec/def :category/keyword ::generators/keyword)

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
    (-> (db/nice-query {:find ['?id]
                        :in {'?category (:db/id entity)}
                        :where '[[_ :product/categories ?category]
                                 [_ :product/images ?id]]})
        (first)
        (:id)
        (entity/find)
        (entity/to-json))))

(entity/register-type!
 :category
 {:attributes
  [{:db/ident :category/parent
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :category/name
    :db/valueType :db.type/ref
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
      :category/name (entities.i18n/get-i18n-entity {:en_US "Default"})
      :category/keyword :default}
     {:schema/type :schema.type/category
      :category/name (entities.i18n/get-i18n-entity {:en_US "Sample category"})
      :category/keyword :sample-category}
     {:schema/type :schema.type/category
      :category/name (entities.i18n/get-i18n-entity {:en_US "Winter"})
      :category/keyword :winter}])

  :dependencies
  #{:file :i18n}

  :to-json
  (fn [this _]
    (-> this
        (assoc :category/image (get-image this))
        ((:to-json entity/default-type))))})
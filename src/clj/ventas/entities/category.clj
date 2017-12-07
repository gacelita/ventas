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
  (spec/with-gen ::entity/ref #(entity/ref-generator :category)))

(spec/def :category/image
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(spec/def :category/keyword ::generators/keyword)

(spec/def :schema.type/category
  (spec/keys :req [:category/name
                   :category/keyword]
             :opt [:category/image
                   :category/parent]))

(defn get-image [entity params]
  (if (:category/image entity)
    (entity/find-json (:category/image entity) params)
    (when-let [image-eid
               (-> (db/nice-query {:find ['?id]
                                   :in {'?category (:db/id entity)}
                                   :where '[[?product :product/categories ?category]
                                            [?image :product.image/product ?product]
                                            [?image :product.image/file ?id]]})
                   (first)
                   (:id))]
      (entity/find-json image-eid params))))

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
    [{:category/name (entities.i18n/get-i18n-entity {:en_US "Default"
                                                     :es_ES "Predeterminada"})
      :category/keyword :default}])

  :dependencies
  #{:file :i18n}

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (assoc :image (get-image this params))))})
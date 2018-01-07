(ns ventas.entities.image-size
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils.images :as utils.images]
   [ventas.entities.file :as entities.file]
   [ventas.paths :as paths]))

(spec/def :image-size/keyword ::generators/keyword)

(spec/def :image-size/width
  (spec/with-gen number?
                 #(gen/choose 100 400)))
(spec/def :image-size/height
  (spec/with-gen number?
                 #(gen/choose 100 400)))

(def algorithms
  #{:image-size.algorithm/resize-only-if-over-maximum
    :image-size.algorithm/always-resize
    :image-size.algorithm/crop-and-resize})

(spec/def :image-size/algorithm algorithms)

(spec/def :image-size/quality
  (spec/with-gen number?
                 #(gen/double*
                   {:infinite? false
                    :NaN? false
                    :min 0.0
                    :max 1.0})))

(def entities
  #{:schema.type/brand
    :schema.type/category
    :schema.type/product
    :schema.type/user})

(spec/def :image-size/entities
  (spec/coll-of entities :kind set?))

(spec/def :schema.type/image-size
  (spec/keys :req [:image-size/keyword
                   :image-size/width
                   :image-size/height
                   :image-size/algorithm
                   :image-size/entities]
             :opt [:image-size/quality]))

(entity/register-type!
 :image-size
 {:attributes
  [{:db/ident :image-size/keyword
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/width
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/height
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/algorithm
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size.algorithm/resize-only-if-over-maximum}
   {:db/ident :image-size.algorithm/always-resize}
   {:db/ident :image-size.algorithm/crop-and-resize}

   {:db/ident :image-size/quality
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/entities
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]

  :fixtures
  (fn []
    [{:schema/type :schema.type/image-size
      :image-size/keyword :admin-products-edit
      :image-size/width 150
      :image-size/height 150
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}

     {:schema/type :schema.type/image-size
      :image-size/keyword :admin-orders-edit-line
      :image-size/width 80
      :image-size/height 80
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}])})

(defn size-entity->configuration [{:image-size/keys [width height algorithm quality]}]
  (let [algorithm (name algorithm)]
    (cond-> {:quality quality
             :progressive true
             :resize {:width width
                      :height height}}
            (= "resize-only-if-over-maximum" algorithm)
              (assoc-in [:resize :allow-smaller?] true)
            (= "crop-and-resize" algorithm)
              (assoc :crop {:relation (/ width height)}))))

(defn transform
  "Transforms a :file entity representing an image, using the configuration
   given by an :image-size entity. Saves the resulting image into the corresponding
   path, and returns given path (just returns the path if nothing has to be done)"
  [file-entity size-entity]
  {:pre [(= (entity/type file-entity) :file)
         (= (entity/type size-entity) :image-size)]}
  (utils.images/transform-image
   (entities.file/filepath file-entity)
   (paths/resolve paths/resized-images)
   (size-entity->configuration size-entity)))
(ns ventas.entities.image-size
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils :as utils]
   [ventas.utils.files :refer [basename]]))

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

(spec/def :image-size/algorithm
  (spec/with-gen
   (spec/or :pull-eid ::db/pull-eid
            :algorithm algorithms)
   #(gen/elements algorithms)))

(spec/def :image-size/quality
  (spec/with-gen number?
    #(gen/double*
      {:infinite? false
       :NaN? false
       :min 0.0
       :max 1.0})))

(spec/def :schema.type/image-size
  (spec/keys :req [:image-size/keyword
                   :image-size/width
                   :image-size/height
                   :image-size/algorithm]
             :opt [:image-size/quality]))

(entity/register-type!
 :image-size
 {:migrations
  [[:base (utils/into-n
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
             :db/cardinality :db.cardinality/one
             :ventas/refEntityType :enum}

            {:db/ident :image-size/quality
             :db/valueType :db.type/float
             :db/cardinality :db.cardinality/one}

            {:db/ident :image-size/entities
             :db/valueType :db.type/ref
             :db/cardinality :db.cardinality/many
             :ventas/refEntityType :enum}]

           (map #(hash-map :db/ident %) algorithms))]
   [:deprecate-entities [{:db/id :image-size/entities
                          :schema/deprecated true
                          :schema/see-instead :file/image-sizes}]]]})
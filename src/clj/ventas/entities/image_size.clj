(ns ventas.entities.image-size
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.file :as entities.file]
   [ventas.paths :as paths]
   [ventas.utils.images :as utils.images]
   [ventas.utils :as utils]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))

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

(def ^:private gen-entities
  #{:schema.type/brand
    :schema.type/category
    :schema.type/product
    :schema.type/user})

(spec/def ::entity
  (spec/with-gen
   (spec/or :pull-eid ::db/pull-eid
            :entity :schema/type)
   #(gen/elements gen-entities)))

(spec/def :image-size/entities
  (spec/coll-of ::entity))

(spec/def :schema.type/image-size
  (spec/keys :req [:image-size/keyword
                   :image-size/width
                   :image-size/height
                   :image-size/algorithm]
             :opt [:image-size/quality
                   :image-size/entities]))

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

           (map #(hash-map :db/ident %) algorithms))]]})

(defn size-entity->configuration [{:image-size/keys [width height algorithm quality]}]
  (let [algorithm (name algorithm)]
    (cond-> {:quality (or quality 1)
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
  (future
   (utils.images/transform-image
    (entities.file/filepath file-entity)
    (paths/resolve paths/resized-images)
    (size-entity->configuration size-entity))))

(defn already-transformed? [file-entity size-entity]
  (let [source-path (entities.file/filepath file-entity)
        options (size-entity->configuration size-entity)]
    (-> (str (paths/resolve paths/resized-images)
             "/" (utils.images/path-with-metadata source-path options))
        io/file
        .exists)))

(defn list-images [entity]
  (entity/call-type-fn ::list-images entity))

(defn get-pending-images []
  (remove nil?
          (for [{:image-size/keys [entities] :as image-size} (entity/query :image-size)
                entity-type entities
                entity (entity/query (keyword (name entity-type)))
                file-entity (map entity/find (list-images entity))]
            (when-not (already-transformed? file-entity image-size)
              {:image-size image-size
               :file file-entity}))))

(defn transform-all []
  (future
   (doseq [{:keys [image-size file]} (get-pending-images)]
     (log/debug {:transforming {:file (:db/id file)
                                   :image-size (:db/id image-size)}})
     @(transform file image-size))))

(defn clean-storage []
  (doseq [file (.listFiles (io/file (paths/resolve ::paths/resized)))]
    (io/delete-file file))
  true)

(defn entities []
  (entity/types-with-property ::list-images))
(ns ventas.entities.file
  (:refer-clojure :exclude [slurp spit])
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.storage :as storage]
   [mount.core :refer [defstate]]
   [ventas.search.indexing :as search.indexing]
   [ventas.utils.images :as utils.images]
   [clojure.tools.logging :as log]
   [ventas.utils.files :as utils.files]
   [clojure.java.io :as io]
   [ventas.database :as db]))

(defn identifier [entity]
  {:pre [(:db/id entity)]}
  (if-let [kw (:file/keyword entity)]
    (name kw)
    (:db/id entity)))

(defn filename [entity]
  (str (identifier entity) "." (:file/extension entity)))

(defn url [entity]
  (str "/files/" (identifier entity)))

(defn spit
  "Copies a file to the corresponding path of a :file entity.
   Does not overwrite the existing file, if any."
  [entity path]
  (storage/put-object (filename entity) path))

(defn slurp
  "Slurps the file corresponding to the :file entity"
  [entity]
  (storage/get-object (filename entity)))

(defn create-from-file!
  "Creates a :file entity from an existing file"
  [source-path extension & [kw]]
  (let [entity (entity/create :file {:extension extension
                                     :keyword kw})]
    (spit entity source-path)
    entity))

(spec/def :file/extension
  (spec/with-gen string? #(spec/gen #{"png"})))

(spec/def :file/keyword ::generators/keyword)

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]
             :opt [:file/keyword]))

(spec/def ::ref
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(def image-extensions #{"jpg" "jpeg" "png" "gif" "webp"})

(defn- image? [file-entity]
  (contains? image-extensions (:file/extension file-entity)))

(defn- image-sizes [file-entity]
  (->> (db/nice-query {:find '[?sizes]
                       :in {'?file-eid (:db/id file-entity)}
                       :where '[(or-join [?file-eid ?sizes]
                                         (and [?fle :file.list.element/file ?file-eid]
                                              [?fl :file.list/elements ?fle]
                                              [?fl :file.list/image-sizes ?sizes])
                                         [?file-eid :file/image-sizes ?sizes])]})
       (map (comp entity/find :sizes))))

(declare transform)

(entity/register-type!
 :file
 {:migrations
  [[:base [{:db/ident :file/extension
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}
           {:db/ident :file/keyword
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}]]
   [:add-image-sizes [{:db/ident :file/image-sizes
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/many
                       :ventas/refEntityType :image-size}]]]

  :autoresolve? true

  :after-transact
  (fn [entity _]
    (doseq [size (image-sizes entity)]
      @(transform entity size)))

  :serialize
  (fn [this params]
    (-> ((entity/default-attr :serialize) this params)
        (assoc :url (url this))))

  :deserialize
  (fn [this]
    (-> this
        (dissoc :url)
        ((entity/default-attr :deserialize))))})

#_ "
  :file.list represents an ordered collection of files.
  :product/images should migrate to this at some point.
"

(spec/def :file.list.element/position number?)

(spec/def :file.list.element/file
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(spec/def :schema.type/file.list.element
  (spec/keys :req [:file.list.element/position
                   :file.list.element/file]))

(entity/register-type!
 :file.list.element
 {:migrations
  [[:base [{:db/ident :file.list.element/position
            :db/valueType :db.type/long
            :db/cardinality :db.cardinality/one}
           {:db/ident :file.list.element/file
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}]]
   [:file-component [{:db/id :file.list.element/file
                      :db/isComponent true}]]]

  :dependencies #{:file}
  :autoresolve? true
  :seed-number 0})

(spec/def :file.list/elements
  (spec/with-gen ::entity/refs #(entity/refs-generator :file.list.element)))

(spec/def :schema.type/file.list
  (spec/keys :req [:file.list/elements]))

(entity/register-type!
 :file.list
 {:migrations
  [[:base [{:db/ident :file.list/elements
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many
            :ventas/refEntityType :file.list.element
            :db/isComponent true}]]
   [:add-image-sizes [{:db/ident :file.list/image-sizes
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/many
                       :ventas/refEntityType :image-size}]]]
  :dependencies #{:file.list.element}
  :autoresolve? true
  :component? true
  :after-transact
  (fn [entity _]
    (let [elements (map entity/find (:file.list/elements entity))]
      (doseq [file (map (comp entity/find :file.list.element/file) elements)]
        (doseq [size (image-sizes file)]
          @(transform file size)))))
  :serialize
  (fn [this params]
    (->> ((entity/default-attr :serialize) this params)
         :elements
         (sort-by :position)
         (map :file)))
  :seed-number 0})

(defn ->list-entity [files]
  {:schema/type :schema.type/file.list
   :file.list/elements (map-indexed
                        (fn [idx file]
                          {:schema/type :schema.type/file.list.element
                           :file.list.element/position idx
                           :file.list.element/file file})
                        files)})

(defmethod search.indexing/transform-entity-by-type :file [entity]
  (entity/serialize entity))

(defmethod search.indexing/transform-entity-by-type :file.list [entity]
  (entity/serialize entity))

;; image resizing

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

(defn resized-file-key [file-entity size-entity]
  (let [options (size-entity->configuration size-entity)]
    (-> (filename file-entity)
        (utils.images/path-with-metadata options)
        (->> (str "resized-images/")))))

(defn already-transformed? [file-entity size-entity]
  (storage/stat-object (resized-file-key file-entity size-entity)))

(defn transform
  "Transforms a :file entity representing an image, using the configuration
   given by an :image-size entity. Saves the resulting image into the corresponding
   path, and returns given path (just returns the path if nothing has to be done)"
  [file-entity size-entity & [{:keys [overwrite?]}]]
  {:pre [(= (entity/type file-entity) :file)
         (= (entity/type size-entity) :image-size)]}
  (log/info "Transforming" (:db/id file-entity) ", size:" (:image-size/keyword size-entity))
  (future
   (let [source-filename (filename file-entity)
         source-file (storage/get-object source-filename)]
     (if-not source-file
       (log/warn "Couldn't transform" (:db/id file-entity) ":" source-filename "not found")
       (let [file-key (resized-file-key file-entity size-entity)]
         (log/debug "File key: "file-key)
         (if (and (not overwrite?) (storage/stat-object file-key))
           (log/info (str (:db/id file-entity)) "already transformed for size" (:image-size/keyword size-entity))
           (let [middle-filepath (str (utils.files/get-tmp-dir) "/" source-filename)
                 _ (io/copy source-file (io/file middle-filepath))
                 path (utils.images/transform-image
                       middle-filepath
                       nil
                       (size-entity->configuration size-entity))]
             (storage/put-object file-key path)))
         file-key)))))

(defn transform-all [& [{:keys [overwrite?] :as opts}]]
  (future
   (doseq [image (entity/query :file {:file/extension [:in image-extensions]})]
     (doseq [size (image-sizes image)]
       @(transform image size opts)))))

(defn clean-storage []
  (doseq [key (storage/list-objects "resized-images")]
    (storage/remove-object key)))
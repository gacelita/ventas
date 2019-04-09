(ns ventas.entities.file
  (:refer-clojure :exclude [slurp spit])
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.storage :as storage]
   [mount.core :refer [defstate]]
   [ventas.search.indexing :as search.indexing]))

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
  (prn :create-from-file! source-path)
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

(entity/register-type!
 :file
 {:migrations
  [[:base [{:db/ident :file/extension
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}
           {:db/ident :file/keyword
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}]]]

  :autoresolve? true

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
            :db/cardinality :db.cardinality/one}]]]

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
            :db/isComponent true}]]]
  :dependencies #{:file.list.element}
  :autoresolve? true
  :component? true
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
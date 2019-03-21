(ns ventas.entities.file
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.paths :as paths]
   [ventas.utils :as utils]
   [ventas.utils.jar :as utils.jar]
   [ventas.search :as search]))

(defn identifier [entity]
  {:pre [(:db/id entity)]}
  (if-let [kw (:file/keyword entity)]
    (name kw)
    (:db/id entity)))

(defn filename [entity]
  (str (identifier entity) "." (:file/extension entity)))

(defn filepath [entity]
  (str (paths/resolve paths/storage) "/" (filename entity)))

(defn url [entity]
  (str "/files/" (identifier entity)))

(defn copy-file!
  "Copies a file to the corresponding path of a :file entity.
   Does not overwrite the existing file, if any."
  [entity path]
  (let [new-path (io/file (filepath entity))]
    (io/make-parents new-path)
    (when-not (.exists new-path)
      (io/copy path new-path))))

(defn create-from-file!
  "Creates a :file entity from an existing file"
  [source-path extension & [kw]]
  (let [entity (entity/create :file {:extension extension
                                     :keyword kw})]
    (copy-file! entity source-path)
    entity))

(spec/def :file/extension
  (spec/with-gen string? #(spec/gen #{"png"})))

(spec/def :file/keyword ::generators/keyword)

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]
             :opt [:file/keyword]))

(spec/def ::ref
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(defn- get-seed-files [extension]
  (let [pattern (str ".*?\\." extension)
        files (utils/find-files (paths/resolve paths/seeds)
                                (re-pattern pattern))]
    (if (seq files)
      files
      (filter #(-> (paths/path->resource (paths/resolve paths/seeds))
                   (str "/files/" pattern)
                   (re-pattern)
                   (re-matches %))
              (utils.jar/list-resources)))))

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

  :after-seed
  (fn [{:file/keys [extension] :as this}]
    (let [seed-files (get-seed-files extension)]
      (when (seq seed-files)
        (copy-file! this (rand-nth seed-files)))))

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

(search/configure-types!
 {:file.list {:indexable? false}
  :file.list.element {:indexable? false}})

(defn ->list-entity [files]
  {:schema/type :schema.type/file.list
   :file.list/elements (map-indexed
                        (fn [idx file]
                          {:schema/type :schema.type/file.list.element
                           :file.list.element/position idx
                           :file.list.element/file file})
                        files)})
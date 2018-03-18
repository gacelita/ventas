(ns ventas.entities.file
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [pantomime.mime :as mime]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.paths :as paths]
   [ventas.utils :as utils]
   [ventas.utils.jar :as utils.jar]))

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
  [source-path]
  (let [mime (mime/mime-type-of (io/file source-path))
        extension (subs (mime/extension-for-name mime) 1)
        entity (entity/create :file {:extension extension})]
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
 {:attributes
  [{:db/ident :file/extension
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :file/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}]

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

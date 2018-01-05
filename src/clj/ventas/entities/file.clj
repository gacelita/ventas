(ns ventas.entities.file
  (:require
    [clojure.java.io :as io]
    [clojure.spec.alpha :as spec]
    [clojure.test.check.generators :as gen]
    [com.gfredericks.test.chuck.generators :as gen']
    [ventas.config :as config]
    [ventas.database :as db]
    [ventas.database.entity :as entity]
    [ventas.paths :as paths]
    [ventas.utils :as utils]
    [ventas.utils.jar :as utils.jar]
    [ventas.database.generators :as generators]
    [pantomime.mime :as mime]))

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
  (let [mime (mime/mime-type-of (clojure.java.io/file source-path))
        extension (subs (mime/extension-for-name mime) 1)
        entity (entity/create :file {:extension extension})
        target-path (str (paths/resolve paths/storage) "/" (:db/id entity) "." extension)]
    (.renameTo
      (clojure.java.io/file source-path)
      (clojure.java.io/file target-path))
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
        files (utils/find-files (str (paths/resolve paths/seeds) "seeds/files") (re-pattern pattern))]
    (if (seq files)
      files
      (filter #(re-matches (re-pattern (str (paths/path->resource (paths/resolve paths/seeds)) "/files/" pattern)) %)
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

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (assoc :url (url this))))

  :from-json
  (fn [this]
    (dissoc this :url))})
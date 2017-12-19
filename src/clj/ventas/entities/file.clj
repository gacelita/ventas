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
   [ventas.database.generators :as generators]))

(defn filename [entity]
  {:pre [(:db/id entity)]}
  (str (if-let [kw (:file/keyword entity)]
         (name kw)
         (:db/id entity))
       "." (:file/extension entity)))

(defn filepath [entity]
  (str paths/images "/" (filename entity)))

(defn copy-file!
  "Copies a file to the corresponding path of a :file entity.
   Does not overwrite the existing file, if any."
  [entity path]
  (let [new-path (io/file (filepath entity))]
    (io/make-parents new-path)
    (when-not (.exists new-path)
      (io/copy path new-path))))

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
        files (utils/find-files (str paths/seeds "seeds/files") (re-pattern pattern))]
    (if (seq files)
      files
      (filter #(re-matches (re-pattern (str (paths/path->resource paths/seeds) "/files/" pattern)) %)
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
        (assoc :url (paths/path->url (filepath this)))))})
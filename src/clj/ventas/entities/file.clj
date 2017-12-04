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
   [ventas.utils :refer [find-files]]))

(defn filename [entity]
  {:pre [(:db/id entity)]}
  (str (:db/id entity) "." (name (:file/extension entity))))

(defn copy-file!
  "Copies a file to the corresponding path of a :file entity"
  [entity path]
  (let [new-path (str paths/images "/" (filename entity))]
    (io/make-parents new-path)
    (io/copy path (io/file new-path))))

(spec/def :file/extension #{:file.extension/jpg :file.extension/gif :file.extension/png :file.extension/tiff})

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]))

(spec/def ::ref
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(entity/register-type!
 :file
 {:attributes
  [{:db/ident :file/extension
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :file.extension/png}
   {:db/ident :file.extension/jpg}
   {:db/ident :file.extension/gif}
   {:db/ident :file.extension/tiff}]

  :filter-seed
  (fn [this]
    (-> this
        (assoc :file/extension :file.extension/jpg)))

  :after-seed
  (fn [this]
    (let [file (rand-nth (find-files (str paths/seeds "/files") (re-pattern ".*?")))]
      (copy-file! this file)))

  :autoresolve? true

  :to-json
  (fn [this _]
    (let [path (str paths/images "/" (filename this))]
      (-> this
          (assoc :url (paths/path->url path))
          ((entity/default-attr :to-json)))))})
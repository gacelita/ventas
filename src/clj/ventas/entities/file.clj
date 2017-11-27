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
   [ventas.util :refer [find-files]]))

(spec/def :file/extension #{:file.extension/jpg :file.extension/gif :file.extension/png :file.extension/tiff})

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]))

(defn filename [entity]
  (str (:db/id entity) "." (name (:file/extension entity))))

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
    (let [file (rand-nth (find-files (str paths/seeds "/files") (re-pattern ".*?")))
          path (str paths/images "/" (filename this))]
      (io/copy file (io/file path))))

  :autoresolve? true

  :to-json
  (fn [this _]
    (let [path (str paths/images "/" (filename this))]
      (-> this
          (assoc :url (paths/path->url path))
          ((:to-json entity/default-type)))))})
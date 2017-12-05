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
   [ventas.utils :refer [find-files]]
   [ventas.database.generators :as generators]))

(defn filename [entity]
  {:pre [(:db/id entity)]}
  (str (:db/id entity) "." (:file/extension entity)))

(defn filepath [entity]
  (str paths/images "/" (filename entity)))

(defn copy-file!
  "Copies a file to the corresponding path of a :file entity"
  [entity path]
  (let [new-path (filepath entity)]
    (io/make-parents new-path)
    (io/copy path (io/file new-path))))

(spec/def :file/extension
  (spec/with-gen string? #(spec/gen #{"jpg" "gif" "png"})))

(spec/def :file/keyword ::generators/keyword)

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]
             :opt [:file/keyword]))

(spec/def ::ref
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

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

  :filter-seed
  (fn [this]
    (-> this
        (assoc :file/extension "jpg")))

  :after-seed
  (fn [this]
    (let [file (rand-nth (find-files (str paths/seeds "/files") (re-pattern ".*?")))]
      (copy-file! this file)))

  :autoresolve? true

  :fixtures
  (fn []
    [{:file/keyword :logo}])

  :to-json
  (fn [this _]
    (let [path (filepath this)]
      (-> this
          (assoc :url (paths/path->url path))
          ((entity/default-attr :to-json)))))})
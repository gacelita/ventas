(ns ventas.entities.file
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.config :as config]
            [clojure.java.io :as io]
            [ventas.util :refer [find-files]]))

(spec/def :file/extension #{:file.extension/jpg :file.extension/gif :file.extension/png :file.extension/tiff})

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]))

(entity/register-type! :file
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
    (let [file (rand-nth (find-files "resources/seeds/files" (re-pattern ".*?")))
          path (str "resources/public/files/img/" (:db/id this) ".jpg")]
      (io/copy file (io/file path))))

  :filter-json
  (fn [this]
    (-> this
        (assoc :url (str (config/get :base-url) "img/" (:db/id this)
                         "." (name (:file/extension this))))))})
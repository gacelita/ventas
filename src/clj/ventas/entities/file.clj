(ns ventas.entities.file
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.config :refer [config]]
            [clojure.java.io :as io]
            [ventas.util :refer [find-files]]))

(spec/def :file/extension #{:file.extension/jpg :file.extension/gif :file.extension/png :file.extension/tiff})

(spec/def :schema.type/file
  (spec/keys :req [:file/extension]))

(entity/register-type! :file
 {:filter-seed
  (fn [this]
    (-> this
        (assoc :file/extension :file.extension/jpg)))
  :after-seed
  (fn [this]
    (let [file (rand-nth (find-files "resources/seeds/files" (re-pattern ".*?")))]
      (io/copy file (io/file (str "resources/public/img/" (:db/id this) ".jpg")))))
  :filter-json
  (fn [this]
    (-> this
        (assoc :url (str (:base-url config) "img/" (:db/id this)
                         "." (name (:file/extension this))))))})
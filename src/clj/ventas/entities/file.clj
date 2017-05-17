(ns ventas.entities.file
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.config :refer [config]]
            [clojure.java.io :as io]
            [ventas.util :refer [find-files]]))

(s/def :file/extension #{:file.extension/jpg :file.extension/gif :file.extension/png :file.extension/tiff})

(s/def :schema.type/file
  (s/keys :req [:file/extension]))

(defmethod db/entity-preseed :file [type entity-data]
  (-> entity-data (assoc :file/extension :file.extension/jpg)))

(defmethod db/entity-postseed :file [entity]
  (let [file (rand-nth (find-files "resources/seeds/files" (re-pattern ".*?")))]
    (io/copy file (io/file (str "resources/public/img/" (:id entity) ".jpg")))))

(defmethod db/entity-json :file [entity]
  (-> entity
      (dissoc :type)
      (dissoc :created-at)
      (dissoc :updated-at)
      (dissoc :extension)
      (assoc :url (str (:base-url config) "img/" (:id entity) "." (name (:extension entity))))))
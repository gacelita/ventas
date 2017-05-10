(ns ventas.entities.file
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :image/extension #{:image.extension/jpg :image.extension/gif :image.extension/png :image.extension/tiff})
(s/def :image/source
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :user)))))
(s/def :schema.type/image
  (s/keys :req [:image/extension :image/source]))



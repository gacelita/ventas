(ns ventas.entities.file
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :file/extension #{:file.extension/jpg :file.extension/gif :file.extension/png :file.extension/tiff})
(s/def :schema.type/file
  (s/keys :req [:file/extension]))



(ns ventas.entities.resource
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :resource/keyword keyword?)
(s/def :resource/name string?)
(s/def :resource/file
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :file)))))

(s/def :schema.type/resource
  (s/keys :req [:resource/keyword]
          :opt [:resource/name]))
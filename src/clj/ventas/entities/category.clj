(ns ventas.entities.category
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :category/name string?)
(s/def :category/parent
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :category)))))

(s/def :category/image
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :file)))))

(s/def :schema.type/category
  (s/keys :req [:category/name]
          :opt [:category/parent :category/image]))

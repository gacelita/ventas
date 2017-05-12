(ns ventas.entities.attribute-value
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :attribute-value/name string?)

(s/def :attribute-value/attribute
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :attribute)))))

(s/def :schema.type/attribute-value
  (s/keys :req [:attribute-value/name :attribute-value/attribute]))

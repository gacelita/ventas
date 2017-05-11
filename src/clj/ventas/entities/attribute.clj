(ns ventas.entities.attribute
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :attribute/name string?)

(s/def :schema.type/attribute
  (s/keys :req [:attribute/name]))



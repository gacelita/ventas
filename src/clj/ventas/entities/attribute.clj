(ns ventas.entities.attribute
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(spec/def :attribute/name string?)

(spec/def :schema.type/attribute
  (spec/keys :req [:attribute/name]))



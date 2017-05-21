(ns ventas.entities.configuration
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :configuration/key keyword?)
(s/def :configuration/value string?)

(s/def :schema.type/configuration
  (s/keys :req [:configuration/key
                :configuration/value]))
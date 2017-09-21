(ns ventas.entities.configuration
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :configuration/key keyword?)
(spec/def :configuration/value string?)

(spec/def :schema.type/configuration
  (spec/keys :req [:configuration/key
                :configuration/value]))

(entity/register-type! :configuration)
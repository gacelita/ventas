(ns ventas.entities.country
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :country/name string?)

(spec/def :schema.type/country
  (spec/keys :req [:country/name]))

(entity/register-type! :country)
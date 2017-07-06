(ns ventas.entities.country
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :country/name string?)

(s/def :schema.type/country
  (s/keys :req [:country/name]))
(ns ventas.entities.state
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :state/name string?)

(s/def :schema.type/state
  (s/keys :req [:state/name]))
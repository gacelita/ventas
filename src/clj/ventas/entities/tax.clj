(ns ventas.entities.tax
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :tax/name string?)
(s/def :tax/type #{:tax.type/percentage :tax.type/amount})
(s/def :tax/amount double?)

(s/def :schema.type/tax
  (s/keys :req [:tax/name :tax/type :tax/amount]))


(ns ventas.entities.tax
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :tax/name string?)
(s/def :tax/kind #{:tax.kind/percentage :tax.kind/amount})
(s/def :tax/amount double?)

(s/def :schema.type/tax
  (s/keys :req [:tax/name :tax/kind :tax/amount]))

(defmethod entity/json :tax [entity]
  (-> entity
    (dissoc :type)))
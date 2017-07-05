(ns ventas.entities.brand
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :brand/name string?)
(s/def :brand/description string?)
(s/def :brand/logo
  (s/with-gen integer? #(gen/elements (map :id (entity/query :file)))))

(s/def :schema.type/brand
  (s/keys :req [:brand/name :brand/description :brand/logo]))


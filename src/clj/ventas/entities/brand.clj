(ns ventas.entities.brand
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :brand/name string?)
(spec/def :brand/description string?)
(spec/def :brand/logo
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :file)))))

(spec/def :schema.type/brand
  (spec/keys :req [:brand/name :brand/description :brand/logo]))


(ns ventas.entities.product-variation
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :product-variation/product
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :product)))))

(spec/def :product-variation/attribute-values
  (spec/with-gen (spec/and (spec/* integer?) #(< (count %) 7) #(> (count %) 2))
              #(gen/vector (gen/elements (map :id (entity/query :attribute-value))))))

(spec/def :schema.type/product-variation
  (spec/keys :req [:product-variation/product
                :product-variation/attribute-values]))

(entity/register-type! :product-variation)
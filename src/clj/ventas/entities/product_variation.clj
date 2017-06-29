(ns ventas.entities.product-variation
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :product-variation/product
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :product)))))

(s/def :product-variation/attribute-values
  (s/with-gen (s/and (s/* integer?) #(< (count %) 7) #(> (count %) 2))
              #(gen/vector (gen/elements (map :id (db/entity-query :attribute-value))))))

(s/def :schema.type/product-variation
  (s/keys :req [:product-variation/product
                :product-variation/attribute-values]))


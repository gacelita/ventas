(ns ventas.entities.product
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]))

(s/def :product/name string?)
(s/def :product/reference string?)
(s/def :product/ean13 string?)
(s/def :product/active boolean?)
(s/def :product/description string?)
(s/def :product/condition #{:product.condition/new :product.condition/used :product.condition/refurbished})
(s/def :product/tags
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :tag)))))
(s/def :product/brand
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :brand)))))
(s/def :product/tax
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :tax)))))
(s/def :schema.type/product
  (s/keys :req [:product/name]
          :opt [:product/reference :product/ean13 :product/active :product/description :product/condition :product/tags :product/brand :product/tax]))

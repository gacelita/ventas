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
(s/def :product/tags (s/and (s/* string?) #(< (count %) 7) #(> (count %) 2)))
(s/def :product/price
  (s/with-gen (s/and bigdec? pos?)
              (fn [] (gen/fmap (fn [d] (BigDecimal. (str d))) (gen/double* {:NaN? false :min 0 :max 999})))))

(s/def :product/brand
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :brand)))))
(s/def :product/tax
  (s/with-gen integer? #(gen/elements (map :id (db/entity-query :tax)))))
(s/def :product/images
  (s/with-gen (s/and (s/* integer?) #(< (count %) 7) #(> (count %) 2))
              #(gen/vector (gen/elements (map :id (db/entity-query :file))))))

;; product:
;;    ...
;; product-variation:
;;    product-variation.price: some specific price
;;    product-variation.name: some specific name
;;    product-variation.product: ref to product
;;    product-variation.attribute-values: list of refs to attribute values
;; attribute:
;;    attribute.name: "Color"
;; attribute-value:
;;    attribute-value.name: "Blue"
;;    attribute-value.attribute: ref to attribute

(s/def :schema.type/product
  (s/keys :req [:product/name
                :product/active
                :product/price]
          :opt [:product/reference
                :product/ean13
                :product/description
                :product/condition
                :product/tags
                :product/brand
                :product/tax
                :product/images]))

(defmethod db/entity-json :product [entity]
  (-> entity
      (dissoc :type)
      (dissoc :created-at)
      (dissoc :updated-at)
      (#(if-let [c (:condition %1)]
          (assoc %1 :condition (keyword (name c)))
          %1))
      (#(if-let [t (:tax %1)]
          (assoc %1 :tax (db/entity-json (db/entity-find t)))
          %1))
      (#(if-let [imgs (:images %1)]
          (assoc %1 :images (map (comp db/entity-json db/entity-find) imgs))
          %1))))
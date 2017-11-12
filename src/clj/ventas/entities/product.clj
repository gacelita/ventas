(ns ventas.entities.product
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :product/name string?)
(spec/def :product/reference string?)
(spec/def :product/ean13 string?)
(spec/def :product/active boolean?)
(spec/def :product/description string?)
(spec/def :product/condition #{:product.condition/new :product.condition/used :product.condition/refurbished})
(spec/def :product/tags (spec/coll-of string?))
(spec/def :product/price
  (spec/with-gen
   (spec/and bigdec? pos?)
   (fn []
     (gen/fmap #(-> % (str) (BigDecimal.))
               (gen/double* {:NaN? false :min 0 :max 999})))))

(spec/def :product/brand
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :brand)))))
(spec/def :product/tax
  (spec/with-gen integer? #(gen/elements (map :db/id (entity/query :tax)))))
(spec/def :product/images
  (spec/with-gen (spec/coll-of integer?)
              #(gen/vector (gen/elements (map :db/id (entity/query :file))))))

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

(spec/def :schema.type/product
  (spec/keys :req [:product/name
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

(entity/register-type!
 :product
 {:attributes
  [{:db/ident :product/price
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/name
    :db/valueType :db.type/string
    :db/index true
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/reference
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/ean13
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/active
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/condition
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product.condition/new}
   {:db/ident :product.condition/used}
   {:db/ident :product.condition/refurbished}

   {:db/ident :product/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/tags
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}

   {:db/ident :product/tax
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/images
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]

  :dependencies
  #{:brand :tax :file}

  :to-json
  (fn [this]
    (-> this
        (update :product/brand (comp entity/to-json entity/find))
        (update :product/tax (comp entity/to-json entity/find))
        (update :product/images #(map (comp entity/to-json entity/find) %))
        ((:to-json entity/default-type))))})
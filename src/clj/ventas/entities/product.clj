(ns ventas.entities.product
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database.generators :as generators]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.util :refer [update-if-exists]]))

(spec/def :product/name ::generators/string)

(spec/def :product/reference ::generators/string)

(spec/def :product/ean13 ::generators/string)

(spec/def :product/active boolean?)

(spec/def :product/description ::generators/string)

(spec/def :product/condition #{:product.condition/new :product.condition/used :product.condition/refurbished})

(spec/def :product/price
  (spec/with-gen
   (spec/and bigdec? pos?)
   (fn []
     (gen/fmap #(-> % (str) (BigDecimal.))
               (gen/double* {:NaN? false :min 0 :max 999})))))

(spec/def :product/brand
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :brand)))

(spec/def :product/tax
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :tax)))

(spec/def :product/images
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :file)))

(spec/def :product/categories
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :category)))

(spec/def :product/terms
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :product.term)))



;; product:
;;    ...
;; product.variation:
;;    product-variation.price: some specific price
;;    product-variation.name: some specific name
;;    product-variation.product: ref to product
;;    product-variation.terms: list of refs to terms
;; product.taxonomy:
;;    taxonomy.name: "Color"
;;    taxonomy.keyword: :color
;; product.term:
;;    term.name: "Blue"
;;    term.taxonomy: ref to attribute

(spec/def ::product-for-generation
  (spec/keys :req [:product/name
                   :product/active
                   :product/price
                   :product/images]
             :opt [:product/reference
                   :product/ean13
                   :product/description
                   :product/condition
                   :product/brand
                   :product/tax
                   :product/categories
                   :product/terms]))

(spec/def :schema.type/product
  (spec/with-gen
   (spec/keys :req [:product/name
                    :product/active
                    :product/price]
              :opt [:product/reference
                    :product/ean13
                    :product/description
                    :product/condition
                    :product/brand
                    :product/tax
                    :product/images
                    :product/categories
                    :product/terms])
   #(spec/gen ::product-for-generation)))

(entity/register-type!
 :product
 {:attributes
  [{:db/ident :product/price
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/index true}

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

   {:db/ident :product/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/images
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :product/tax
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/brand
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/condition
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :product.condition/new}
   {:db/ident :product.condition/used}
   {:db/ident :product.condition/refurbished}

   {:db/ident :product/categories
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :product/terms
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]

  :dependencies
  #{:brand :tax :file :category :product.term}

  :to-json
  (fn [this]
    (-> this
        (update-if-exists :product/brand (comp entity/to-json entity/find))
        (update-if-exists :product/tax (comp entity/to-json entity/find))
        (update-if-exists :product/images #(map (comp entity/to-json entity/find) %))
        ((:to-json entity/default-type))))})
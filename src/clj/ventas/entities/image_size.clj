(ns ventas.entities.image-size
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]))

(spec/def :image-size/width
  (spec/with-gen number?
                 #(gen/choose 100 400)))
(spec/def :image-size/height
  (spec/with-gen number?
                 #(gen/choose 100 400)))

(spec/def
  :image-size/algorithm
  #{:image-size.algorithm/resize-only-if-over-maximum
    :image-size.algorithm/always-resize
    :image-size.algorithm/crop-and-resize})

(spec/def :image-size/quality
  (spec/with-gen number?
                 #(gen/double*
                   {:infinite? false
                    :NaN? false
                    :min 0.0
                    :max 1.0})))

(spec/def
  :image-size/entities
  (spec/coll-of #{:schema.type/brand
                  :schema.type/category
                  :schema.type/product
                  :schema.type/product.variation
                  :schema.type/resource
                  :schema.type/user}
                :kind set?))

(spec/def :schema.type/image-size
  (spec/keys :req [:image-size/width
                   :image-size/height
                   :image-size/algorithm
                   :image-size/quality
                   :image-size/entities]))

(entity/register-type!
 :image-size
 {:attributes
  [{:db/ident :image-size/width
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/height
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/algorithm
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size.algorithm/resize-only-if-over-maximum}
   {:db/ident :image-size.algorithm/always-resize}
   {:db/ident :image-size.algorithm/crop-and-resize}

   {:db/ident :image-size/quality
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one}

   {:db/ident :image-size/entities
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}]})
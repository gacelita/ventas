(ns ventas.entities.product
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [ventas.database.generators :as generators]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :as utils]
   [ventas.utils.files :as utils.files]
   [ventas.entities.file :as entities.file]
   [clojure.java.io :as io]))

(spec/def :product/name ::entities.i18n/ref)

(spec/def :product/reference ::generators/string)

(spec/def :product/ean13 ::generators/string)

(spec/def :product/active boolean?)

(spec/def :product/description ::entities.i18n/ref)

(spec/def :product/keyword ::generators/keyword)

(spec/def :product/condition #{:product.condition/new
                               :product.condition/used
                               :product.condition/refurbished})

(spec/def :product/price
  (spec/with-gen
   (spec/and utils/bigdec? pos?)
   (fn []
     (gen/fmap #(-> % (str) (BigDecimal.))
               (gen/double* {:NaN? false :min 0 :max 999})))))

(spec/def :product/brand
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :brand)))

(spec/def :product/tax
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :tax)))

(spec/def :product/categories
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :category)))

(spec/def :product/terms
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :product.term)))

(spec/def ::product-for-generation
  (spec/keys :req [:product/name
                   :product/active
                   :product/price
                   :product/keyword]
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
                    :product/price
                    :product/keyword]
              :opt [:product/reference
                    :product/ean13
                    :product/description
                    :product/condition
                    :product/brand
                    :product/tax
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
    :db/valueType :db.type/ref
    :db/index true
    :db/cardinality :db.cardinality/one}

   {:db/ident :product/keyword
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
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
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

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

  :fixtures
  (fn []
    [{:product/name (entities.i18n/get-i18n-entity {:en_US "Test product"})
      :product/active true
      :product/price 15.4M
      :product/reference "REF001"
      :product/ean13 "7501031311309"
      :product/description (entities.i18n/get-i18n-entity {:en_US "This is a test product"})
      :product/condition :product.condition/new
      :product/brand [:brand/keyword :test-brand]
      :product/tax [:tax/keyword :test-tax]
      :product/categories [[:category/keyword :test-category]]
      :product/terms [[:product.term/keyword :green-color]]
      :product/keyword :test-product}])

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (assoc :images (->> (entity/query :product.image {:product (:db/id this)})
                            (map entity/to-json)
                            (map (fn [{:keys [file position]}]
                                   (assoc file :position position)))
                            (sort-by :position)
                            (into [])))))})


(spec/def :product.image/position number?)

(spec/def :product.image/file
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(spec/def :product.image/product
  (spec/with-gen ::entity/ref #(entity/ref-generator :product)))

(spec/def :schema.type/product.image
  (spec/keys :req [:product.image/position
                   :product.image/file
                   :product.image/product]))

(entity/register-type!
 :product.image
 {:attributes
  [{:db/ident :product.image/position
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :product.image/file
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :product.image/product
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:file :product}

  :seed-number 0

  :fixtures
  (fn []
    [{:product.image/position 0
      :product.image/file (:db/id (first (entity/query :file)))
      :product.image/product [:product/keyword :test-product]}])})

(defn add-image
  "Meant for development"
  [product-eid path]
  (let [product (entity/find product-eid)
        file {:file/extension (utils.files/extension path)
              :schema/type :schema.type/file}
        image {:schema/type :schema.type/product.image
               :product.image/position 0
               :product.image/file file
               :product.image/product product-eid}
        {:product.image/keys [file]} (entity/create* image)]
    (entities.file/copy-file!
     (entity/find file)
     (io/file path))))
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
   [ventas.common.utils :as common.utils]
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
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :amount)))

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

(spec/def :product/images
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :product.image)))

(spec/def :product/variation-terms
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :product.term)))

(spec/def :product/parent
  (spec/with-gen ::entity/ref
                 #(entity/ref-generator :product)))

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
                   :product/terms
                   :product/variation-terms
                   :product/parent]))

(spec/def :schema.type/product
  (spec/with-gen
   (spec/keys :req [:product/name
                    :product/price]
              :opt [:product/reference
                    :product/ean13
                    :product/description
                    :product/condition
                    :product/brand
                    :product/tax
                    :product/categories
                    :product/terms
                    :product/variation-terms
                    :product/parent
                    :product/keyword
                    :product/active])
   #(spec/gen ::product-for-generation)))

(defn- terms-to-json [terms]
  (->> terms
       (common.utils/group-by-keyword :taxonomy)
       (map (fn [[taxonomy terms]]
              {:taxonomy taxonomy :terms terms}))))

(entity/register-type!
 :product
 {:attributes
  [{:db/ident :product/price
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/ident :product/name
    :db/valueType :db.type/ref
    :db/index true
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

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
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

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
    :db/cardinality :db.cardinality/many}

   {:db/ident :product/images
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident :product/variation-terms
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :product/parent
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:brand :tax :file :category :product.term :amount}

  :autoresolve? true

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (update :terms terms-to-json)
        (update :variation-terms terms-to-json)
        (update :images (fn [images]
                          (->> images
                               (map (fn [{:keys [file position]}]
                                      (assoc file :position position)))
                               (sort-by :position)
                               (map #(dissoc % :position))
                               (into []))))))})

(spec/def :product.image/position number?)

(spec/def :product.image/file
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(spec/def :schema.type/product.image
  (spec/keys :req [:product.image/position
                   :product.image/file]))

(entity/register-type!
 :product.image
 {:attributes
  [{:db/ident :product.image/position
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :product.image/file
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:file}

  :autoresolve? true

  :seed-number 0})

(spec/def :product.variation/parent
  (spec/with-gen ::entity/ref #(entity/ref-generator :product)))

(spec/def :product.variation/terms
  (spec/with-gen ::entity/refs #(entity/refs-generator :product.term)))

(spec/def :schema.type/product.variation
  (spec/keys :req [:product.variation/parent
                   :product.variation/terms]))

(defn- variation-to-json* [all-terms selected-terms params]
  (let [selected-terms (->> selected-terms
                            (map #(entity/find-json % params))
                            (common.utils/group-by-keyword :taxonomy))]
    (map (fn [{:keys [taxonomy] :as item}]
           (assoc item :selected (->> selected-terms
                                      (common.utils/find-first (fn [[k v]] (= (:id k) (:id taxonomy))))
                                      (second)
                                      (first))))
         all-terms)))

(entity/register-type!
 :product.variation
 {:attributes
  [{:db/ident :product.variation/parent
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :product.variation/terms
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :product.variation/default?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:product :product.term}

  :seed-number 0
  :autoresolve? true

  :to-json
  (fn [this params]
    (let [product (entity/find (:product.variation/parent this))
          attrs (->> this
                     (filter (fn [[k v]]
                               (= (namespace k) "product")))
                     (into {}))
          product-json (entity/to-json (merge product attrs) params)]
      (-> product-json
          (assoc :variation (variation-to-json* (:variation-terms product-json)
                                                (:product.variation/terms this)
                                                params))
          (dissoc :variation-terms)
          (assoc :id (:db/id this)))))})


(defn add-image
  "Meant for development"
  [product-eid path]
  (let [product (entity/find product-eid)
        file {:file/extension (utils.files/extension path)
              :schema/type :schema.type/file}
        image {:schema/type :schema.type/product.image
               :product.image/position 0
               :product.image/file file}
        {:product.image/keys [file] :db/keys [id]} (entity/create* image)]
    (entity/update* {:db/id product-eid
                     :product/images id}
                    :append? true)
    (entities.file/copy-file!
     (entity/find file)
     (io/file path))))

(defn- find-variation* [ref terms]
  (if-let [variation (entity/query-one :product.variation {:parent ref
                                                           :terms terms})]
    variation
    (entity/create :product.variation {:parent ref
                                       :terms terms})))

(defn find-variation
  "Tries to find a variation for the product with the given `eid`, with the given `terms`.
   If no terms are given, the default variation is returned.
   If the terms given do not have a corresponding variation, it will be created.
   This function not returning an entity is considered a bug."
  [ref & [terms]]
  {:pre [(utils/check ::entity/ref ref)]}
  (let [terms (set terms)]
    (if (empty? terms)
      (if-let [default (entity/query-one :product.variation {:default? true
                                                             :parent ref})]
        default
        (find-variation* ref terms))
      (find-variation* ref terms))))
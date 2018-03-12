(ns ventas.entities.category
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :refer [update-if-exists]]
   [ventas.utils.slugs :as utils.slugs]))

(spec/def :category/name ::entities.i18n/ref)

(spec/def :category/parent
  (spec/with-gen ::entity/ref #(entity/ref-generator :category)))

(spec/def :category/image
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(spec/def :category/keyword ::generators/keyword)

(spec/def :schema.type/category
  (spec/keys :req [:category/name
                   :category/keyword]
             :opt [:category/image
                   :category/parent]))

(defn get-image [entity & [params]]
  (if (:category/image entity)
    (entity/find-json (:category/image entity) params)
    (when-let [image-eid
               (-> (db/nice-query {:find ['?id]
                                   :in {'?category (:db/id entity)}
                                   :where '[[?product :product/categories ?category]
                                            [?product :product/images ?image]
                                            [?image :product.image/file ?id]]})
                   first
                   :id)]
      (entity/find-json image-eid params))))

(defn get-parents [id]
  (let [id (db/normalize-ref id)
        {:category/keys [parent]} (entity/find id)]
    (if parent
      (conj (get-parents parent) id)
      #{id})))

(defn get-parent-slug [ref]
  {:pre [ref]}
  (let [slug (-> (if (entity/entity? ref)
                   ref
                   (entity/find ref))
                 (utils.slugs/add-slug-to-entity :category/name)
                 :ventas/slug)]
    (if (map? slug)
      slug
      (entity/find-recursively slug))))

(defn- add-slug-to-category [entity]
  (let [source (:category/name entity)]
    (if (and (not (:ventas/slug entity)) source)
      (assoc
       entity
       :ventas/slug
       (let [slug (utils.slugs/slugify-i18n source)]
         (if-let [parent (:category/parent entity)]
           (entities.i18n/merge-i18ns-with #(str/join "-" [%1 %2])
                                           (get-parent-slug parent)
                                           slug)
           slug)))
      entity)))

(entity/register-type!
 :category
 {:attributes
  [{:db/ident :category/parent
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :category/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :ventas/refEntityType :i18n}

   {:db/ident :category/image
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :category/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}]

  :filter-create

  (fn [this]
    (add-slug-to-category this))

  :filter-update
  (fn [_ attrs]
    (add-slug-to-category attrs))

  :dependencies
  #{:file :i18n}

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (assoc :image (get-image this params))))

  :from-json
  (fn [this]
    (-> this
        (dissoc :image)
        ((entity/default-attr :from-json))))})

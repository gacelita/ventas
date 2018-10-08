(ns ventas.entities.category
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :refer [update-if-exists]]
   [ventas.utils.slugs :as utils.slugs]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.common.utils :as common.utils]
   [clojure.set :as set]
   [clojure.data :as data]))

(spec/def :category/name ::entities.i18n/ref)

(spec/def :category/parent
  (spec/with-gen ::entity/ref #(entity/ref-generator :category)))

(spec/def :category/image
  (spec/with-gen ::entity/ref #(entity/ref-generator :file)))

(spec/def :category/keyword ::generators/keyword)

(spec/def :schema.type/category
  (spec/keys :req [:category/name]
             :opt [:category/image
                   :category/parent
                   :category/keyword]))

(defn get-image [entity & [params]]
  (if (:category/image entity)
    (entity/find-serialize (:category/image entity) params)
    (when-let [image-eid (db/nice-query-attr
                          {:find ['?id]
                           :in {'?category (:db/id entity)}
                           :where '[[?product :product/categories ?category]
                                    [?product :product/images ?image]
                                    [?image :product.image/file ?id]]})]
      (entity/find-serialize image-eid params))))

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

(defn- options* [category-id culture & [cache]]
  (let [{:category/keys [parent name]} (entity/find category-id)
        name (entity/serialize (entity/find name) {:culture culture})]
    (if parent
      (let [result (if (get cache parent)
                     cache
                     (options* parent culture))]
        (merge cache
               result
               {category-id (str (get result parent) " / " name)}))
      (assoc cache category-id name))))

(defn leaves []
  (map first
       (db/q
        {:find '[?id]
         :in '[$ ?type]
         :where '[[?id :schema/type ?type]
                  (not [_ :category/parent ?id])]}
        [:schema.type/category])))

(defn branches []
  (map first
       (db/q
        {:find '[?id]
         :in '[$ ?type]
         :where '[[?id :schema/type ?type]
                  [_ :category/parent ?id]]}
        [:schema.type/category])))

(defn options-without-branches
  "Like options but excludes branches (categories that have children)"
  [culture]
  (let [branches-map (->> (branches)
                          (reduce (fn [cache id]
                                    (options* id culture cache))
                                  {})
                          (into {}))
        leaves-map (->> (leaves)
                        (reduce (fn [cache id]
                                  (options* id culture cache))
                                branches-map)
                        (into {}))]
    (first (data/diff leaves-map branches-map))))

(defn options
  "Returns a map of ids to the full names of the corresponding categories.
   Given an example category 17592186046253, returns:
   {17592186046253 'Women / Shirts'}"
  [culture & [ids]]
  (->> (or ids (leaves))
       (reduce (fn [cache id]
                 (options* id culture cache))
               {})
       (into {})))

(defn full-name-i18n
  "Returns an i18n entity representing the full name of a category"
  [category-id]
  (let [{:category/keys [parent name]} (entity/find category-id)
        name (entity/find name)]
    (if-not parent
      name
      (dissoc (entities.i18n/merge-i18ns-with #(str/join " / " [%1 %2])
                                              (full-name-i18n parent)
                                              name)
              :db/id))))

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

  :serialize
  (fn [this params]
    (-> ((entity/default-attr :serialize) this params)
        (assoc :image (get-image this params))
        (assoc :full-name (entity/serialize (full-name-i18n (:db/id this)) params))))

  :deserialize
  (fn [this]
    (-> this
        (dissoc :image)
        ((entity/default-attr :deserialize))))

  ::entities.image-size/list-images
  (fn [{:keys [:category/image]}]
    [image])})

(ns ventas.entities.menu
  (:require
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.utils :as utils]
   [ventas.database.generators :as generators]
   [clojure.spec.alpha :as spec]))

(spec/def :menu/name ::entities.i18n/ref)

(spec/def :menu/items
  (spec/with-gen ::entity/refs
                 #(entity/refs-generator :menu.item)))

(spec/def :schema.type/menu
  (spec/keys :req [:menu/name
                   :menu/items]))

(spec/def :menu.item/name ::entities.i18n/ref)

(spec/def :menu.item/position integer?)

(spec/def :menu.item/link ::generators/string)

(spec/def :menu.item/children
  (spec/with-gen ::entity/refs
                 #(entity/ref-generator :menu.item)))

(spec/def :schema.type/menu-item
  (spec/keys :req [:menu.item/name
                   :menu.item/link
                   :menu.item/position]
             :opt [:menu.item/children]))

(entity/register-type!
 :menu.item
 {:migrations
  [[:base (utils/into-n
           [{:db/ident :menu.item/name
             :db/valueType :db.type/ref
             :db/cardinality :db.cardinality/one
             :db/isComponent true
             :ventas/refEntityType :i18n}
            {:db/ident :menu.item/link
             :db/valueType :db.type/string
             :db/cardinality :db.cardinality/one}
            {:db/ident :menu.item/position
             :db/valueType :db.type/long
             :db/cardinality :db.cardinality/one}
            {:db/ident :menu.item/children
             :db/valueType :db.type/ref
             :db/isComponent true
             :db/cardinality :db.cardinality/many
             :ventas/refEntityType :menu.item}])]]

  :autoresolve? true

  :filter-create
  (fn [this]
    (update this :menu.item/link pr-str))

  :filter-update
  (fn [_ attrs]
    (update attrs :menu.item/link pr-str))

  :filter-query
  (fn [this]
    (update this :menu.item/link read-string))

  :serialize
  (fn [this params]
    (-> ((entity/default-attr :serialize) this params)
        (update :children #(sort-by :position %))))

  :dependencies
  #{:i18n}})

(entity/register-type!
 :menu
 {:migrations
  [[:base (utils/into-n
           [{:db/ident :menu/name
             :db/valueType :db.type/ref
             :db/cardinality :db.cardinality/one
             :db/isComponent true
             :ventas/refEntityType :i18n}

            {:db/ident :menu/items
             :db/valueType :db.type/ref
             :db/isComponent true
             :db/cardinality :db.cardinality/many
             :ventas/refEntityType :menu.item}])]]

  :autoresolve? true

  :serialize
  (fn [this params]
    (-> ((entity/default-attr :serialize) this params)
        (update :items #(sort-by :position %))))

  :dependencies
  #{:i18n :menu.item}})
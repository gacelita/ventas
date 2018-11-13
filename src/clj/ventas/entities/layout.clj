(ns ventas.entities.layout
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]))

#_"
Theme settings
  Font, colors, favicon, social media, checkout
Sections
  Home page
  Password page
  Product pages
  Collection pages
  Collections list
  Blogs
  Cart
  404 page
Header
  Logo and logo alignment and width
  Menu
  Preheader / notice / \"announcement bar\"

Footer
  Newsletter
  Menu
  Text
"

(comment
 {:schema/type :schema.type/layout
  :layout/theme :clothing
  :layout/blocks [{:schema/type :schema.type/layout.block
                   :layout.block/config 17592186045602
                   :layout.block/position 0
                   :layout.block/section :home}]})

(spec/def :layout.block/config ::entity/ref)
(spec/def :layout.block/position integer?)
(spec/def :layout.block/section ::generators/keyword)

(spec/def :schema.type/layout.block
  (spec/keys :req [:layout.block/config
                   :layout.block/position
                   :layout.block/section]))

(entity/register-type!
 :layout.block
 {:attributes
  [{:db/ident :layout.block/config
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :layout.block/widget
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :layout.block/position
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :layout.block/section
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}]

  :autoresolve? true

  :dependencies
  #{:i18n}})

(spec/def :layout/theme ::generators/keyword)
(spec/def :layout/blocks
  (spec/coll-of
   (spec/with-gen ::entity/ref #(entity/ref-generator :layout.block))))

(spec/def :schema.type/layout
  (spec/keys :req [:layout/theme
                   :layout/blocks]))

(entity/register-type!
 :layout
 {:attributes
  [{:db/ident :layout/theme
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :layout/blocks
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}]

  :autoresolve? true

  :dependencies
  #{:layout.block}})
(ns ventas.themes.clothing.core
  (:require
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.theme :as theme]
   [clojure.spec.alpha :as spec]))

(theme/register!
 :clothing
 {:version "0.1"
  :name "Clothing"
  :fixtures
  (fn []
    [{:schema/type :schema.type/image-size
      :image-size/keyword :product-page-vertical-carousel
      :image-size/width 120
      :image-size/height 180
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}

     {:schema/type :schema.type/image-size
      :image-size/keyword :product-page-main
      :image-size/width 460
      :image-size/height 650
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}

     {:schema/type :schema.type/image-size
      :image-size/keyword :product-page-main-zoom
      :image-size/width (* 460 4)
      :image-size/height (* 650 4)
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}])})

(spec/def :product.term/color ::generators/string)

(theme/register-migration!
 :clothing
 [{:db/ident :product.term/color
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one}])
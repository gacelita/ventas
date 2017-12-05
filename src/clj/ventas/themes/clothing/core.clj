(ns ventas.themes.clothing.core
  (:require
   [ventas.database.entity :as entity]
   [ventas.theme :as theme]))

(theme/register!
 :clothing
 {:version "0.1"
  :name "Clothing"
  :fixtures
  (fn []
    {:schema/type :schema.type/image-size
     :image-size/keyword :product-page-vertical-carousel
     :image-size/width 120
     :image-size/height 180
     :image-size/algorithm :image-size.algorithm/crop-and-resize
     :image-size/entities #{:schema.type/product}})})
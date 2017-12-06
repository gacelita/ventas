(ns ventas.themes.clothing.core
  (:require
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.theme :as theme]
   [clojure.spec.alpha :as spec]
   [ventas.entities.i18n :as entities.i18n]))

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
      :image-size/entities #{:schema.type/product}}

     {:schema/type :schema.type/product.term
      :product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark green"
                                                         :es_ES "Verde oscuro"})
      :product.term/keyword :dark-green
      :product.term/color "#584838"
      :product.term/taxonomy [:product.taxonomy/keyword :color]}

     {:schema/type :schema.type/product.term
      :product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark red"
                                                         :es_ES "Rojo oscuro"})
      :product.term/keyword :dark-red
      :product.term/color "#5f3239"
      :product.term/taxonomy [:product.taxonomy/keyword :color]}

     {:schema/type :schema.type/product.term
      :product.term/name (entities.i18n/get-i18n-entity {:en_US "Black"
                                                         :es_ES "Negro"})
      :product.term/keyword :black
      :product.term/color "#252525"
      :product.term/taxonomy [:product.taxonomy/keyword :color]}

     {:schema/type :schema.type/product.term
      :product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark blue"
                                                         :es_ES "Azul oscuro"})
      :product.term/keyword :dark-blue
      :product.term/color "#262b3f"
      :product.term/taxonomy [:product.taxonomy/keyword :color]}])})

(spec/def :product.term/color ::generators/string)

(theme/register-migration!
 :clothing
 [{:db/ident :product.term/color
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one}])
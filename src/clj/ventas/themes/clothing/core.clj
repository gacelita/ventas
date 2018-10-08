(ns ventas.themes.clothing.core
  "This theme is being developed at the same time as the platform itself.
   For now it's meant for development and demo purposes, in the future it will be
   included as one of the stock themes.
   Includes demo data to ease development (although ventas is perfectly fine generating
   entities for you, they tend to be not `real world enough` for development)"
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.generators :as generators]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.i18n :as i18n :refer [i18n]]
   [ventas.seo :as seo]
   [ventas.theme :as theme]
   [ventas.themes.clothing.demo :as demo]
   [ventas.utils :as utils]
   [ventas.plugins.menu.core :as menu]
   [ventas.search.entities :as search.entities]
   [ventas.database.entity :as entity]
   [clojure.string :as str]))

(spec/def :product.term/color ::generators/string)

(i18n/register-translations!
 {:en_US {::products "Products"
          ::categories "Categories"
          ::home "Home"}})

(menu/setup!
 {:find-routes
  (fn find-routes [input culture]
    (let [input (if input (str/lower-case input) "")
          culture-kw (entities.i18n/culture->kw culture)]
      (->> (into
            (->> [{:route [:frontend]
                   :name (i18n culture-kw ::home)
                   :type :page}]
                 (filter #(str/includes? (str/lower-case (:name %)) input)))
            (->> (search.entities/search input
                                         #{:product/name
                                           :category/name}
                                         culture)
                 (map (fn [{:keys [id name full-name type images]}]
                        (let [route (case type
                                      :product :frontend.product
                                      :category :frontend.category)]
                          {:route [route :id id]
                           :name (case type
                                   :product name
                                   :category full-name)
                           :type type
                           :image (first images)}))))))))
  :route->name
  (fn route->name [[route _ id] culture]
    (let [culture-kw (entities.i18n/culture->kw culture)]
      (case route
        :frontend (i18n culture-kw ::home)
        :frontend.product (:name (entity/find-serialize id {:culture culture}))
        :frontend.category (:full-name (entity/find-serialize id {:culture culture})))))})

(theme/register!
 :clothing
 {:name "Clothing"
  :build {:modules {:main {:entries ['ventas.themes.clothing.core]}}}
  :prerendered-routes
  (fn []
    (utils/into-n
     [[:frontend]
      [:frontend.privacy-policy]
      [:frontend.login]]
     (->> (seo/type->slugs :schema.type/category)
          (map (fn [{:keys [slug-value]}]
                 [:frontend.category :id slug-value])))
     (->> (seo/type->slugs :schema.type/product)
          (map (fn [{:keys [slug-value]}]
                 [:frontend.product :id slug-value])))))
  :fixtures
  (fn []
    (utils/into-n
     [;; Themes should declare the image sizes they will use as fixtures.
      {:schema/type :schema.type/image-size
       :image-size/keyword :product-page-vertical-carousel
       :image-size/width 120
       :image-size/height 180
       :image-size/algorithm :image-size.algorithm/crop-and-resize
       :image-size/entities #{:schema.type/product}}

      {:schema/type :schema.type/image-size
       :image-size/keyword :product-page-horizontal-carousel
       :image-size/width 360
       :image-size/height 540
       :image-size/algorithm :image-size.algorithm/crop-and-resize
       :image-size/entities #{:schema.type/product}}

      {:schema/type :schema.type/image-size
       :image-size/keyword :header-search
       :image-size/width 50
       :image-size/height 50
       :image-size/algorithm :image-size.algorithm/crop-and-resize
       :image-size/entities #{:schema.type/product}}

      {:schema/type :schema.type/image-size
       :image-size/keyword :cart-page-line
       :image-size/width 145
       :image-size/height 215
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

      {:schema/type :schema.type/image-size
       :image-size/keyword :product-listing
       :image-size/width 270
       :image-size/height 405
       :image-size/algorithm :image-size.algorithm/crop-and-resize
       :image-size/entities #{:schema.type/product}}

      {:schema/type :schema.type/image-size
       :image-size/keyword :category-listing
       :image-size/width 270
       :image-size/height 350
       :image-size/algorithm :image-size.algorithm/crop-and-resize
       :image-size/entities #{:schema.type/product}}

      {:schema/type :schema.type/image-size
       :image-size/keyword :logo
       :image-size/width 300
       :image-size/height 100
       :image-size/algorithm :image-size.algorithm/crop-and-resize}

      ;; Themes can also include taxonomies
      {:schema/type :schema.type/product.taxonomy
       :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "Color"
                                                              :es_ES "Color"})
       :product.taxonomy/keyword :color}

      {:schema/type :schema.type/product.taxonomy
       :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "Size"
                                                              :es_ES "Talla"})
       :product.taxonomy/keyword :size}

      {:schema/type :schema.type/product.taxonomy
       :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "Shoes size"
                                                              :es_ES "Talla de zapatos"})
       :product.taxonomy/keyword :shoes-size}

      {:schema/type :schema.type/product.taxonomy
       :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "Length"
                                                              :es_ES "Longitud"})
       :product.taxonomy/keyword :length}

      {:schema/type :schema.type/product.taxonomy
       :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "Lace type"
                                                              :es_ES "Tipo de cordones"})
       :product.taxonomy/keyword :lace-type}]

     (demo/demo-data)))

  :migrations
  [;; Extends the default terms with colors, which is something that
   ;; other themes may not care about (think of a seafood store)
   [{:db/ident :product.term/color
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}]]})

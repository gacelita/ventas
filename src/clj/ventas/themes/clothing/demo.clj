(ns ventas.themes.clothing.demo
  "Demo data"
  (:require
   [ventas.entities.i18n :as entities.i18n]
   [ventas.entities.amount :as entities.amount]))

(defn product-terms []
  (map #(assoc % :schema/type :schema.type/product.term)
       [{:product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark green"
                                                            :es_ES "Verde oscuro"})
         :product.term/keyword :color-dark-green
         :product.term/color "#584838"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark red"
                                                            :es_ES "Rojo oscuro"})
         :product.term/keyword :color-dark-red
         :product.term/color "#5f3239"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark blue"
                                                            :es_ES "Azul oscuro"})
         :product.term/keyword :color-dark-blue
         :product.term/color "#262b3f"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Black"
                                                            :es_ES "Negro"})
         :product.term/keyword :color-black
         :product.term/color "#252525"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Green"
                                                            :es_ES "Verde"})
         :product.term/keyword :color-green
         :product.term/color "#3b8c16"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Red"
                                                            :es_ES "Rojo"})
         :product.term/keyword :color-red
         :product.term/color "#c60d0d"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Blue"
                                                            :es_ES "Azul"})
         :product.term/keyword :color-blue
         :product.term/color "#1c43af"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Long"
                                                            :es_ES "Largo"})
         :product.term/keyword :length-long
         :product.term/taxonomy [:product.taxonomy/keyword :length]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Short"
                                                            :es_ES "Corto"})
         :product.term/keyword :length-short
         :product.term/taxonomy [:product.taxonomy/keyword :length]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "38"
                                                            :es_ES "38"})
         :product.term/keyword :shoes-size-38
         :product.term/taxonomy [:product.taxonomy/keyword :shoes-size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "40"
                                                            :es_ES "40"})
         :product.term/keyword :shoes-size-40
         :product.term/taxonomy [:product.taxonomy/keyword :shoes-size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "41"
                                                            :es_ES "41"})
         :product.term/keyword :shoes-size-41
         :product.term/taxonomy [:product.taxonomy/keyword :shoes-size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "None"
                                                            :es_ES "Ninguno"})
         :product.term/keyword :lace-type-none
         :product.term/taxonomy [:product.taxonomy/keyword :lace-type]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Regular"
                                                            :es_ES "Normal"})
         :product.term/keyword :lace-type-regular
         :product.term/taxonomy [:product.taxonomy/keyword :lace-type]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "XS"
                                                            :es_ES "XS"})
         :product.term/keyword :size-x-small
         :product.term/taxonomy [:product.taxonomy/keyword :size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "S"
                                                            :es_ES "S"})
         :product.term/keyword :size-small
         :product.term/taxonomy [:product.taxonomy/keyword :size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "M"
                                                            :es_ES "M"})
         :product.term/keyword :size-medium
         :product.term/taxonomy [:product.taxonomy/keyword :size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "L"
                                                            :es_ES "L"})
         :product.term/keyword :size-large
         :product.term/taxonomy [:product.taxonomy/keyword :size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "XL"
                                                            :es_ES "XL"})
         :product.term/keyword :size-x-large
         :product.term/taxonomy [:product.taxonomy/keyword :size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "XXL"
                                                            :es_ES "XXL"})
         :product.term/keyword :size-xx-large
         :product.term/taxonomy [:product.taxonomy/keyword :size]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "3XL"
                                                            :es_ES "3XL"})
         :product.term/keyword :size-3x-large
         :product.term/taxonomy [:product.taxonomy/keyword :size]}]))

(defn brands []
  (map #(assoc % :schema/type :schema.type/brand)
       [{:brand/name (entities.i18n/get-i18n-entity {:en_US "Test brand"})
         :brand/keyword :test-brand
         :brand/description (entities.i18n/get-i18n-entity {:en_US "This is the description of the test brand"})
         :brand/logo [:file/keyword :test-brand-logo]}

        {:brand/name (entities.i18n/get-i18n-entity {:en_US "Profitable Brand"})
         :brand/keyword :profitable-brand
         :brand/description (entities.i18n/get-i18n-entity {:en_US "A very profitable brand"})}

        {:brand/name (entities.i18n/get-i18n-entity {:en_US "Terrible Brand"})
         :brand/keyword :terrible-brand
         :brand/description (entities.i18n/get-i18n-entity {:en_US "A terrible brand"})}]))

(defn- process-category [[kw i18n & children] & [parent-kw]]
  (let [kw (keyword (str (when parent-kw
                           (str (name parent-kw) "."))
                         (name kw)))]
    (concat
     [(merge {:category/name (entities.i18n/get-i18n-entity i18n)
              :category/keyword kw
              :schema/type :schema.type/category}
             (when parent-kw
               {:category/parent [:category/keyword parent-kw]}))]
     (mapcat #(process-category % kw) children))))

(defn categories []
  (mapcat process-category
          [[:men {:en_US "Men" :es_ES "Hombre"}
            [:jackets {:en_US "Jackets" :es_ES "Chaquetas"}]
            [:hoodies {:en_US "Hoodies" :es_ES "Sudaderas"}]
            [:sweaters {:en_US "Sweaters" :es_ES "Jerséis"}]
            [:jeans {:en_US "Jeans" :es_ES "Vaqueros"}]
            [:pants {:en_US "Pants" :es_ES "Pantalones"}]
            [:shirts {:en_US "Shirts" :es_ES "Camisetas"}]
            [:shoes {:en_US "Shoes" :es_ES "Calzado"}]]
           [:women {:en_US "Women" :es_ES "Mujer"}
            [:dresses {:en_US "Dresses" :es_ES "Vestidos"}]
            [:jackets {:en_US "Jackets" :es_ES "Chaquetas"}]
            [:hoodies {:en_US "Hoodies" :es_ES "Sudaderas"}]
            [:sweaters {:en_US "Sweaters" :es_ES "Jerséis"}]
            [:jeans {:en_US "Jeans" :es_ES "Vaqueros"}]
            [:pants {:en_US "Pants" :es_ES "Pantalones"}]
            [:shirts {:en_US "Shirts" :es_ES "Camisetas"}]
            [:shoes {:en_US "Shoes" :es_ES "Calzado"}]]
           [:children {:en_US "Children" :es_ES "Niños"}]]))

(defn files []
  (map #(assoc % :schema/type :schema.type/file)
       [{:file/keyword :logo
         :file/extension "png"}
        {:file/keyword :test-product-image
         :file/extension "png"}
        {:file/keyword :test-brand-logo
         :file/extension "png"}]))

(defn taxes []
  (map #(assoc % :schema/type :schema.type/tax)
       [{:tax/name (entities.i18n/get-i18n-entity {:en_US "Test tax"})
         :tax/amount (entities.amount/get-entity 21.0M :eur)
         :tax/kind :tax.kind/percentage
         :tax/keyword :test-tax}]))

(defn products []
  (map #(assoc % :schema/type :schema.type/product)
       [{:product/name (entities.i18n/get-i18n-entity {:en_US "Test product"})
         :product/active true
         :product/price (entities.amount/get-entity 15.4M :eur)
         :product/reference "REF001"
         :product/ean13 "7501031311309"
         :product/description (entities.i18n/get-i18n-entity {:en_US "This is a test product"})
         :product/condition :product.condition/new
         :product/brand [:brand/keyword :test-brand]
         :product/tax [:tax/keyword :test-tax]
         :product/categories [[:category/keyword :men]]
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-dark-green]
                                   [:product.term/keyword :color-dark-red]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-small]]
         :product/keyword :test-product}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Dress one"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.dresses]]
         :product/keyword :dress-1
         :product/brand [:brand/keyword :profitable-brand]
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :color-red]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-small]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :dress-1-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :dress-1-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Dress two"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.dresses]]
         :product/keyword :dress-2
         :product/brand [:brand/keyword :profitable-brand]
         :product/terms [[:product.term/keyword :length-short]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :color-green]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :dress-2-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :dress-2-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Jacket one"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.jackets]]
         :product/keyword :jacket-1
         :product/brand [:brand/keyword :profitable-brand]
         :product/terms [[:product.term/keyword :length-short]]
         :product/variation-terms [[:product.term/keyword :color-green]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-small]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-1-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-1-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Jacket two"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.jackets]]
         :product/keyword :jacket-2
         :product/brand [:brand/keyword :terrible-brand]
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-blue]
                                   [:product.term/keyword :color-red]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-3x-large]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-small]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-2-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-2-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-2-3
                                                :file/extension "jpg"}}
                          {:product.image/position 3
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-2-4
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Jacket three"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.jackets]]
         :product/keyword :jacket-3
         :product/brand [:brand/keyword :terrible-brand]
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-blue]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-3-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-3-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Jacket four"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.jackets]]
         :product/keyword :jacket-4
         :product/brand [:brand/keyword :terrible-brand]
         :product/terms [[:product.term/keyword :length-short]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-4-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-4-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-4-3
                                                :file/extension "jpg"}}
                          {:product.image/position 3
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-4-4
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Jacket five"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.jackets]]
         :product/keyword :jacket-5
         :product/brand [:brand/keyword :profitable-brand]
         :product/variation-terms [[:product.term/keyword :color-blue]
                                   [:product.term/keyword :color-red]
                                   [:product.term/keyword :color-black]
                                   [:product.term/keyword :color-green]
                                   [:product.term/keyword :color-dark-blue]
                                   [:product.term/keyword :color-dark-red]
                                   [:product.term/keyword :size-x-large]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-small]
                                   [:product.term/keyword :size-x-small]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-5-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-5-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Jeans one"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.pants]]
         :product/keyword :jeans-1
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-medium]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jeans-1-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jeans-1-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Shirt one"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.shirts]]
         :product/keyword :shirt-1
         :product/brand [:brand/keyword :profitable-brand]
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-1-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-1-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-1-3
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Shirt two"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.shirts]]
         :product/keyword :shirt-2
         :product/brand [:brand/keyword :profitable-brand]
         :product/terms [[:product.term/keyword :length-short]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :color-dark-red]
                                   [:product.term/keyword :size-large]
                                   [:product.term/keyword :size-medium]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-2-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-2-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-2-3
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Shirt three"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.shirts]]
         :product/keyword :shirt-3
         :product/terms [[:product.term/keyword :length-short]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-3-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shirt-3-2
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Shoes one"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.shoes]]
         :product/keyword :shoes-1
         :product/terms [[:product.term/keyword :lace-type-regular]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :color-dark-red]
                                   [:product.term/keyword :shoes-size-40]
                                   [:product.term/keyword :shoes-size-38]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-1-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-1-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-1-3
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Shoes two"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.shoes]]
         :product/keyword :shoes-2
         :product/brand [:brand/keyword :terrible-brand]
         :product/terms [[:product.term/keyword :lace-type-none]]
         :product/variation-terms [[:product.term/keyword :color-blue]
                                   [:product.term/keyword :shoes-size-40]
                                   [:product.term/keyword :shoes-size-41]
                                   [:product.term/keyword :shoes-size-38]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-2-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-2-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-2-3
                                                :file/extension "jpg"}}
                          {:product.image/position 3
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-2-4
                                                :file/extension "jpg"}}
                          {:product.image/position 4
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :shoes-2-5
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Sweater one"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.sweaters]]
         :product/keyword :sweater-1
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-1-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-1-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-1-3
                                                :file/extension "jpg"}}
                          {:product.image/position 3
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-1-4
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Sweater two"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :women.sweaters]]
         :product/keyword :sweater-2
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-2-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-2-2
                                                :file/extension "jpg"}}
                          {:product.image/position 2
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :jacket-3-1
                                                :file/extension "jpg"}}]}

        {:product/name (entities.i18n/get-i18n-entity {:en_US "Sweater three"})
         :product/active true
         :product/price (entities.amount/get-entity 10M :eur)
         :product/categories [[:category/keyword :men.sweaters]]
         :product/keyword :sweater-3
         :product/terms [[:product.term/keyword :length-long]]
         :product/variation-terms [[:product.term/keyword :color-black]
                                   [:product.term/keyword :size-large]]
         :product/images [{:product.image/position 0
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-3-1
                                                :file/extension "jpg"}}
                          {:product.image/position 1
                           :schema/type :schema.type/product.image
                           :product.image/file {:schema/type :schema.type/file
                                                :file/keyword :sweater-3-2
                                                :file/extension "jpg"}}]}]))

(defn product-variations []
  (map #(assoc % :schema/type :schema.type/product.variation)
       [{:product.variation/parent [:product/keyword :test-product]
         :product.variation/terms [[:product.term/keyword :color-dark-green]
                                   [:product.term/keyword :size-large]]
         :product.variation/default? true
         :product/price (entities.amount/get-entity 19M :eur)
         :product/name (entities.i18n/get-i18n-entity
                        {:en_US "Test product (dark green and large variation)"})}]))

(defn users []
  (map #(assoc % :schema/type :schema.type/user)
       [{:user/first-name "Test"
         :user/last-name "User"
         :user/company "Test Company"
         :user/email "test@test.com"
         :user/status :user.status/active
         :user/password "test"
         :user/phone "+34 654 543 431"
         :user/roles #{:user.role/administrator}
         :user/favorites [[:product/keyword :test-product]]
         :user/culture [:i18n.culture/keyword :en_US]}

        {:user/first-name "Paul"
         :user/last-name "Becker"
         :user/company "Important Company Inc."
         :user/email "paul@gmail.com"
         :user/status :user.status/active
         :user/password "paulbecker"
         :user/phone "666 555 444"
         :user/culture [:i18n.culture/keyword :es_ES]}

        {:user/first-name "Bob"
         :user/last-name "Smith"
         :user/company "Bad Company Inc."
         :user/email "bobsmith@gmail.com"
         :user/status :user.status/active
         :user/password "bobsmith"
         :user/phone "4444 33 222 333"
         :user/culture [:i18n.culture/keyword :en_US]}]))

(defn country-groups []
  (map #(assoc % :schema/type :schema.type/country.group)
       [{:country.group/keyword :test-country-group
         :country.group/name (entities.i18n/get-i18n-entity {:en_US "Test country group"})}]))

(defn countries []
  (map #(assoc % :schema/type :schema.type/country)
       [{:country/keyword :test
         :country/group [:country.group/keyword :test-country-group]
         :country/name (entities.i18n/get-i18n-entity {:en_US "Test country"})}]))

(defn states []
  (map #(assoc % :schema/type :schema.type/state)
       [{:state/keyword :test
         :state/country [:country/keyword :test]
         :state/name (entities.i18n/get-i18n-entity {:en_US "Test state"})}]))

(defn addresses []
  (map #(assoc % :schema/type :schema.type/address)
       [{:address/first-name "Test"
         :address/last-name "Address"
         :address/company "Test Address Company"
         :address/address "Test Street, 210"
         :address/address-second-line "5º A"
         :address/zip "67943"
         :address/city "Test City"
         :address/country [:country/keyword :test]
         :address/state [:state/keyword :test]
         :address/user [:user/email "test@test.com"]}]))

(defn discounts []
  (map #(assoc % :schema/type :schema.type/discount)
       [{:discount/name (entities.i18n/get-i18n-entity {:en_US "Test discount"})
         :discount/code "TEST"
         :discount/active? true
         :discount/max-uses-per-customer 2
         :discount/max-uses 3
         :discount/free-shipping? true
         :discount/amount.tax-included? false
         :discount/amount.kind :discount.amount.kind/percentage
         :discount/amount (entities.amount/get-entity 15M :eur)}]))

(defn shipping-methods []
  (map #(assoc % :schema/type :schema.type/shipping-method)
       [{:shipping-method/name (entities.i18n/get-i18n-entity {:en_US "Default"
                                                               :es_ES "Predeterminado"})
         :shipping-method/default? true
         :shipping-method/manipulation-fee (entities.amount/get-entity 2M :eur)
         :shipping-method/pricing :shipping-method.pricing/price
         :shipping-method/prices [{:schema/type :schema.type/shipping-method.price
                                   :shipping-method.price/country-groups #{[:country.group/keyword :test-country-group]}
                                   :shipping-method.price/amount (entities.amount/get-entity 5M :eur)
                                   :shipping-method.price/min-value 0M}]}]))

(defn demo-data []
  (concat (product-terms)
          (files)
          (brands)
          (categories)
          (taxes)
          (discounts)
          (products)
          (product-variations)
          (users)
          (country-groups)
          (countries)
          (states)
          (addresses)
          (shipping-methods)))

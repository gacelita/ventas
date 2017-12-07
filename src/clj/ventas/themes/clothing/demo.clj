(ns ventas.themes.clothing.demo
  "Demo data"
  (:require [ventas.entities.i18n :as entities.i18n]
            [ventas.database.entity :as entity]))

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

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Black"
                                                            :es_ES "Negro"})
         :product.term/keyword :color-black
         :product.term/color "#252525"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

        {:product.term/name (entities.i18n/get-i18n-entity {:en_US "Dark blue"
                                                            :es_ES "Azul oscuro"})
         :product.term/keyword :color-dark-blue
         :product.term/color "#262b3f"
         :product.term/taxonomy [:product.taxonomy/keyword :color]}

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
         :brand/logo [:file/keyword :test-brand-logo]}]))

(defn categories []
  (map #(assoc % :schema/type :schema.type/category)
       [{:category/name (entities.i18n/get-i18n-entity {:en_US "Man"
                                                        :es_ES "Hombre"})
         :category/keyword :man}
        {:category/name (entities.i18n/get-i18n-entity {:en_US "Woman"
                                                        :es_ES "Mujer"})
         :category/keyword :woman}
        {:category/name (entities.i18n/get-i18n-entity {:en_US "Children"
                                                        :es_ES "Niños"})
         :category/keyword :children}]))

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
         :tax/amount 21.0
         :tax/kind :tax.kind/percentage
         :tax/keyword :test-tax}]))

(defn products []
  (map #(assoc % :schema/type :schema.type/product)
       [{:product/name (entities.i18n/get-i18n-entity {:en_US "Test product"})
         :product/active true
         :product/price {:schema/type :schema.type/product.price
                         :product.price/amount 15.4M
                         :product.price/currency [:currency/keyword :eur]}
         :product/reference "REF001"
         :product/ean13 "7501031311309"
         :product/description (entities.i18n/get-i18n-entity {:en_US "This is a test product"})
         :product/condition :product.condition/new
         :product/brand [:brand/keyword :test-brand]
         :product/tax [:tax/keyword :test-tax]
         :product/categories [[:category/keyword :man]]
         :product/terms [[:product.term/keyword :color-dark-green]
                         [:product.term/keyword :color-dark-red]
                         [:product.term/keyword :size-large]
                         [:product.term/keyword :size-medium]
                         [:product.term/keyword :size-small]]
         :product/keyword :test-product}]))

(defn product-variations []
  (map #(assoc % :schema/type :schema.type/product.variation)
       [{:product.variation/parent [:product/keyword :test-product]
         :product.variation/terms [[:product.term/keyword :color-dark-green]
                                   [:product.term/keyword :size-large]]
         :product/name (entities.i18n/get-i18n-entity {:en_US "Test product (dark green and large variation)"})}]))

(defn product-images []
  (map #(assoc % :schema/type :schema.type/product.image)
       [{:product.image/position 0
         :product.image/file [:file/keyword :test-product-image]
         :product.image/product [:product/keyword :test-product]}]))

(defn users []
  (map #(assoc % :schema/type :schema.type/user)
       [{:user/first-name "Test"
         :user/last-name "User"
         :user/company "Test Company"
         :user/email "test@test.com"
         :user/status :user.status/active
         :user/password "test"
         :user/phone "+34 654 543 431"
         :user/favorites [[:product/keyword :test-product]]
         :user/culture [:i18n.culture/keyword :en_US]}]))

(defn addresses []
  (map #(assoc % :schema/type :schema.type/address)
       [{:address/first-name "Test"
         :address/last-name "Address"
         :address/company "Test Address Company"
         :address/address "Test Street, 210"
         :address/address-second-line "5º A"
         :address/zip "67943"
         :address/city "Test City"
         :address/country (-> (entity/query :country) first :db/id)
         :address/state (-> (entity/query :state) first :db/id)
         :address/user [:user/email "test@test.com"]}]))

(defn demo-data []
  (concat (product-terms)
          (files)
          (brands)
          (categories)
          (taxes)
          (products)
          (product-variations)
          (product-images)
          (users)
          (addresses)))
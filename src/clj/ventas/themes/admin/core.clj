(ns ventas.themes.admin.core
  (:require [ventas.theme :as theme]
            [clojure.string :as str]))

(theme/register!
 :admin
 {:build {:modules {:main {:entries ['ventas.themes.admin.core]}}}
  :should-load? (fn [{:keys [uri]}]
                  (str/starts-with? uri "/admin"))
  :fixtures
  (fn []
    [{:schema/type :schema.type/image-size
      :image-size/keyword :admin-products-edit
      :image-size/width 150
      :image-size/height 150
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}

     {:schema/type :schema.type/image-size
      :image-size/keyword :admin-orders-edit-line
      :image-size/width 80
      :image-size/height 80
      :image-size/algorithm :image-size.algorithm/crop-and-resize
      :image-size/entities #{:schema.type/product}}])})

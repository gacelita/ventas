(ns ventas.i18n
  (:require
   [tongue.core :as tongue]
   [ventas.utils.logging :refer [debug]]))

(def dicts
  {:en_US

   {:ventas.pages.admin.products/create-product "Create product"
    :ventas.pages.admin.products/name "Name"
    :ventas.pages.admin.products/email "Email"
    :ventas.pages.admin.products/actions "Actions"
    :ventas.pages.admin.products/no-products "No products yet"

    :ventas.pages.admin/page "Administration"

    :ventas.pages.admin.users/page "Users"

    :ventas.pages.admin.users.edit/page "Edit user"
    :ventas.pages.admin.users.edit/user-saved-notification "Usuario guardado"

    :ventas.pages.admin.products/page "Products"

    :ventas.pages.admin.products.edit/page "Edit product"
    :ventas.pages.admin.products.edit/product-saved-notification "Producto guardado"
    :ventas.pages.admin.products.edit/name "Name"
    :ventas.pages.admin.products.edit/active "Active"
    :ventas.pages.admin.products.edit/price "Price"
    :ventas.pages.admin.products.edit/reference "Reference"
    :ventas.pages.admin.products.edit/ean13 "EAN13"
    :ventas.pages.admin.products.edit/description "Description"
    :ventas.pages.admin.products.edit/tags "Tags"
    :ventas.pages.admin.products.edit/brand "Brand"
    :ventas.pages.admin.products.edit/tax "Tax"
    :ventas.pages.admin.products.edit/images "Images"
    :ventas.pages.admin.products.edit/send "Send"

    :user.role/administrator "Administrator"
    :user.role/user "User"}

   :tongue/fallback :en_US})

(def ^:private translation-fn (tongue/build-translate dicts))

(defn i18n [kw & args]
  (translation-fn :en_US kw))
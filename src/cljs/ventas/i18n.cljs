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
    :ventas.pages.admin.products/page "Products"
    :ventas.pages.admin.products.edit/page "Edit product"

    :user.role/administrator "Administrator"
    :user.role/user "User"}

   :tongue/fallback :en_US})

(def ^:private translation-fn (tongue/build-translate dicts))

(defn i18n [kw]
  (translation-fn :en_US kw))
(ns ventas.i18n
  "Tongue wrapper.
   For the moment this ns contains the translations, future plan is to move them
   somewhere else"
  (:require
   [tongue.core :as tongue]
   [ventas.common.utils :as common.utils]
   [ventas.utils.goog :as utils.goog]))

(def ^:private base-dicts
  {:en_US
   {:ventas.components.base/loading "Loading"

    :ventas.components.cart/product-added "Product added!"

    :ventas.components.error/no-data "Nothing found!"

    :ventas.components.table/no-rows "No items to show"

    :ventas.components.product-filters/category "Category"

    :ventas.events/session-started "Welcome!"

    :ventas.page/not-found "404"

    :ventas.pages.admin.configuration.image-sizes/actions "Actions"
    :ventas.pages.admin.configuration.image-sizes/create "Create image size"
    :ventas.pages.admin.configuration.image-sizes/width "Width"
    :ventas.pages.admin.configuration.image-sizes/height "Height"
    :ventas.pages.admin.configuration.image-sizes/algorithm "Algorithm"
    :ventas.pages.admin.configuration.image-sizes/keyword "ID"

    :ventas.pages.admin.configuration.image-sizes.edit/algorithm "Algorithm"
    :ventas.pages.admin.configuration.image-sizes.edit/name "Name"
    :ventas.pages.admin.configuration.image-sizes.edit/keyword "Keyword"
    :ventas.pages.admin.configuration.image-sizes.edit/width "Width"
    :ventas.pages.admin.configuration.image-sizes.edit/height "Height"
    :ventas.pages.admin.configuration.image-sizes.edit/entities "Entities"
    :ventas.pages.admin.configuration.image-sizes.edit/amount "Amount"
    :ventas.pages.admin.configuration.image-sizes.edit/submit "Submit"
    :ventas.pages.admin.configuration.image-sizes.edit/saved-notification "Saved!"

    :ventas.pages.admin.products/create-product "Create product"
    :ventas.pages.admin.products/name "Name"
    :ventas.pages.admin.products/price "Price"
    :ventas.pages.admin.products/actions "Actions"
    :ventas.pages.admin.products/no-products "No products yet"

    :ventas.pages.admin/page "Administration"

    :ventas.pages.admin.users/page "Users"

    :ventas.pages.admin.users.edit/page "Edit user"
    :ventas.pages.admin.users.edit/user-saved-notification "User saved"

    :ventas.pages.admin/nothing-here "(Empty page)"

    :ventas.pages.admin.plugins/name "Name"
    :ventas.pages.admin.plugins/version "Version"

    :ventas.pages.admin.products/page "Products"

    :ventas.pages.admin.products.edit/active "Active"
    :ventas.pages.admin.products.edit/brand "Brand"
    :ventas.pages.admin.products.edit/description "Description"
    :ventas.pages.admin.products.edit/ean13 "EAN13"
    :ventas.pages.admin.products.edit/variation-terms "Variation terms"
    :ventas.pages.admin.products.edit/terms "Terms"
    :ventas.pages.admin.products.edit/images "Images"
    :ventas.pages.admin.products.edit/name "Name"
    :ventas.pages.admin.products.edit/page "Edit product"
    :ventas.pages.admin.products.edit/price "Price"
    :ventas.pages.admin.products.edit/product-saved-notification "Product saved"
    :ventas.pages.admin.products.edit/reference "Reference"
    :ventas.pages.admin.products.edit/send "Send"
    :ventas.pages.admin.products.edit/tags "Tags"
    :ventas.pages.admin.products.edit/tax "Tax"

    :ventas.pages.admin.skeleton/email "Email"
    :ventas.pages.admin.skeleton/dashboard "Dashboard"
    :ventas.pages.admin.skeleton/home "The store"
    :ventas.pages.admin.skeleton/password "Password"
    :ventas.pages.admin.skeleton/login "Login"
    :ventas.pages.admin.skeleton/logout "Logout"
    :ventas.pages.admin.skeleton/activity-log "Activity log"
    :ventas.pages.admin.skeleton/administration "Administration"
    :ventas.pages.admin.skeleton/configuration "Configuration"
    :ventas.pages.admin.skeleton/image-sizes "Image sizes"
    :ventas.pages.admin.skeleton/nothing-here "Nothing here"
    :ventas.pages.admin.skeleton/plugins "Plugins"
    :ventas.pages.admin.skeleton/products "Products"
    :ventas.pages.admin.skeleton/users "Users"
    :ventas.pages.admin.skeleton/taxes "Taxes"
    :ventas.pages.admin.skeleton/orders "Orders"
    :ventas.pages.admin.skeleton/payment-methods "Billing"

    :ventas.pages.admin.taxes/name "Name"
    :ventas.pages.admin.taxes/amount "Amount"
    :ventas.pages.admin.taxes/actions "Actions"
    :ventas.pages.admin.taxes/create "Create tax"

    :ventas.pages.admin.orders/user "User"
    :ventas.pages.admin.orders/status "Status"
    :ventas.pages.admin.orders/amount "Amount"
    :ventas.pages.admin.orders/actions "Actions"
    :ventas.pages.admin.orders/create "Create order"

    :ventas.pages.admin.orders.edit/user "User"
    :ventas.pages.admin.orders.edit/status "Status"
    :ventas.pages.admin.orders.edit/payment-reference "Payment reference"
    :ventas.pages.admin.orders.edit/submit "Submit"
    :ventas.pages.admin.orders.edit/order "Order"
    :ventas.pages.admin.orders.edit/billing "Billing"
    :ventas.pages.admin.orders.edit/payment-method "Payment method"
    :ventas.pages.admin.orders.edit/billing-address "Payment address"
    :ventas.pages.admin.orders.edit/shipping "Shipping"
    :ventas.pages.admin.orders.edit/shipping-method "Shipping method"
    :ventas.pages.admin.orders.edit/shipping-address "Shipping address"
    :ventas.pages.admin.orders.edit/shipping-comments "Comments"
    :ventas.pages.admin.orders.edit/lines "Order lines"
    :ventas.pages.admin.orders.edit/image "Image"
    :ventas.pages.admin.orders.edit/name "Name"
    :ventas.pages.admin.orders.edit/price "Price"
    :ventas.pages.admin.orders.edit/quantity "Quantity"
    :ventas.pages.admin.orders.edit/total "Total"

    :ventas.pages.admin.taxes.edit/amount "Amount"
    :ventas.pages.admin.taxes.edit/name "Name"
    :ventas.pages.admin.taxes.edit/kind "Type"
    :ventas.pages.admin.taxes.edit/submit "Submit"

    :ventas.pages.admin.users/no-items "No items to show"
    :ventas.pages.admin.users/new-user "Create"
    :ventas.pages.admin.users/name "Name"
    :ventas.pages.admin.users/email "Email"
    :ventas.pages.admin.users/actions "Actions"

    :ventas.pages.admin.users.edit/first-name "First name"
    :ventas.pages.admin.users.edit/last-name "Last name"
    :ventas.pages.admin.users.edit/company "Company"
    :ventas.pages.admin.users.edit/phone "Phone"
    :ventas.pages.admin.users.edit/email "Email"
    :ventas.pages.admin.users.edit/roles "Roles"
    :ventas.pages.admin.users.edit/submit "Submit"
    :ventas.pages.admin.users.edit/culture "Culture"
    :ventas.pages.admin.users.edit/status "Status"

    :ventas.pages.admin.activity-log/whats-the-activity-log "In the activity log you can see everything that happens in your store. Filter the messages by category to find the information you want."
    :ventas.pages.admin.activity-log/entity-id "ID"
    :ventas.pages.admin.activity-log/entity-type "Entity type"
    :ventas.pages.admin.activity-log/type "Event type"

    :ventas.session/unregistered-error "You need to be a registered user to do that"

    :ventas.utils.formatting/percentage "%"
    :ventas.utils.formatting/amount ""

    :image-size.algorithm/always-resize "Always resize"
    :image-size.algorithm/crop-and-resize "Crop and resize"
    :image-size.algorithm/resize-only-if-over-maximum "Resize only if over maximum"

    :user.role/administrator "Administrator"
    :user.role/user "User"

    :user.status/inactive "Inactive"
    :user.status/pending "Pending"
    :user.status/active "Active"
    :user.status/cancelled "Cancelled"

    :order.status/draft "Draft"
    :order.status/acknowledged "Acknowledged"
    :order.status/paid "Paid"
    :order.status/ready "Ready"
    :order.status/shipped "Shipped"
    :order.status/unpaid "Unpaid"

    :entity.create "Creation"
    :entity.delete "Deletion"
    :entity.update "Update"

    :schema.type/category "Category"
    :schema.type/user "User"
    :schema.type/product "Product"
    :schema.type/brand "Brand"

    :tax.kind/amount "Amount"
    :tax.kind/percentage "Percentage"}

   :tongue/fallback :en_US})

(def dicts (atom base-dicts))

(defn- build-translation-fn* []
  (tongue/build-translate @dicts))

(def ^:private translation-fn (atom (build-translation-fn*)))

(defn- build-translation-fn! []
  (reset! translation-fn (build-translation-fn*)))

(defn register-translations! [m]
  (swap! dicts common.utils/deep-merge m)
  (build-translation-fn!))

(defn i18n [kw & args]
  (apply utils.goog/format (@translation-fn :en_US kw) args))

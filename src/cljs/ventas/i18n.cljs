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

    :ventas.components.form/search "Search"

    :ventas.components.table/no-rows "No items to show"

    :ventas.components.product-filters/category "Category"

    :ventas.components.notificator/saved "Saved!"
    :ventas.components.notificator/error "An error occurred."

    :ventas.events/session-started "Welcome!"

    :ventas.page/not-found "404"

    :ventas.pages.admin.configuration.email/email.from "From address"
    :ventas.pages.admin.configuration.email/email.encryption.enabled "Enable encryption?"
    :ventas.pages.admin.configuration.email/email.encryption.type "Encryption type"
    :ventas.pages.admin.configuration.email/email.smtp.enabled "Use SMTP server?"
    :ventas.pages.admin.configuration.email/email.smtp.host "SMTP host"
    :ventas.pages.admin.configuration.email/email.smtp.port "SMTP port"
    :ventas.pages.admin.configuration.email/email.smtp.user "SMTP user"
    :ventas.pages.admin.configuration.email/email.smtp.password "SMTP password"
    :ventas.pages.admin.configuration.email/submit "Submit"
    :ventas.pages.admin.configuration.email/ssl "SSL"
    :ventas.pages.admin.configuration.email/tls "TLS"
    :ventas.pages.admin.configuration.email/saved "Saved!"
    :ventas.pages.admin.configuration.email/page "Email configuration"

    :ventas.pages.admin.configuration.image-sizes/page "Image sizes"
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

    :ventas.pages.admin.dashboard/traffic-statistics "Traffic statistics"
    :ventas.pages.admin.dashboard/latest-users "Latest users"
    :ventas.pages.admin.dashboard/pending-orders "Pending orders"
    :ventas.pages.admin.dashboard/unread-messages "Unread messages"
    :ventas.pages.admin.dashboard/no-name "(No name)"
    :ventas.pages.admin.dashboard/page "Dashboard"

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
    :ventas.pages.admin.plugins/page "Plugins"
    :ventas.pages.admin.plugins/version "Version"

    :ventas.pages.admin.products/page "Products"

    :ventas.pages.admin.skeleton/discounts "Discounts"

    :ventas.pages.admin.shipping-methods/create "Create"
    :ventas.pages.admin.shipping-methods/name "Name"
    :ventas.pages.admin.shipping-methods/page "Shipping methods"
    :ventas.pages.admin.shipping-methods/actions "Actions"

    :ventas.pages.admin.shipping-methods.edit/page "Edit shipping method"
    :ventas.pages.admin.shipping-methods.edit/name "Name"
    :ventas.pages.admin.shipping-methods.edit/submit "Submit"
    :ventas.pages.admin.shipping-methods.edit/pricing "Pricing"
    :ventas.pages.admin.shipping-methods.edit/default? "Default?"
    :ventas.pages.admin.shipping-methods.edit/manipulation-fee "Manipulation fee"

    :ventas.pages.admin.payment-methods/page "Billing"

    :ventas.pages.admin.products.discounts/name "Name"
    :ventas.pages.admin.products.discounts/page "Discounts"
    :ventas.pages.admin.products.discounts/code "Code"
    :ventas.pages.admin.products.discounts/amount "Amount"
    :ventas.pages.admin.products.discounts/actions "Actions"
    :ventas.pages.admin.products.discounts/create "Create"

    :ventas.pages.admin.products.discounts.edit/name "Name"
    :ventas.pages.admin.products.discounts.edit/page "Edit discount"
    :ventas.pages.admin.products.discounts.edit/code "Code"
    :ventas.pages.admin.products.discounts.edit/active? "Active"
    :ventas.pages.admin.products.discounts.edit/max-uses-per-customer "Max uses per customer"
    :ventas.pages.admin.products.discounts.edit/max-uses "Max uses"
    :ventas.pages.admin.products.discounts.edit/free-shipping? "Free shipping"
    :ventas.pages.admin.products.discounts.edit/product "Product"
    :ventas.pages.admin.products.discounts.edit/amount "Amount"
    :ventas.pages.admin.products.discounts.edit/amount.tax-included? "Tax included?"
    :ventas.pages.admin.products.discounts.edit/amount.kind "Kind"
    :ventas.pages.admin.products.discounts.edit/submit "Submit"

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
    :ventas.pages.admin.skeleton/shipping-methods "Shipping"
    :ventas.pages.admin.skeleton/statistics "Statistics"

    :ventas.pages.admin.statistics/page "Statistics"
    :ventas.pages.admin.statistics/traffic-statistics "Traffic statistics"
    :ventas.pages.admin.statistics/datepicker-placeholder "Date range"
    :ventas.pages.admin.statistics/granularity.amount "Granularity amount"
    :ventas.pages.admin.statistics/granularity.type "Granularity type"
    :ventas.pages.admin.statistics/twenty-four-hours "24h"
    :ventas.pages.admin.statistics/week "Week"
    :ventas.pages.admin.statistics/month "Month"
    :ventas.pages.admin.statistics/custom "Historical"
    :ventas.pages.admin.statistics/realtime "Realtime"
    :ventas.pages.admin.statistics/show-data-from "Show data from"
    :ventas.pages.admin.statistics/minutes "Minutes"
    :ventas.pages.admin.statistics/hours "Hours"
    :ventas.pages.admin.statistics/days "Days"
    :ventas.pages.admin.statistics/weeks "Weeks"
    :ventas.pages.admin.statistics/months "Months"
    :ventas.pages.admin.statistics/seconds "Seconds"
    :ventas.pages.admin.statistics/show-last "Show last"
    :ventas.pages.admin.statistics/every "every"
    :ventas.pages.admin.statistics/minute "minute"
    :ventas.pages.admin.statistics/hour "hour"
    :ventas.pages.admin.statistics/navigation-events "Navigation events"
    :ventas.pages.admin.statistics/http-events "HTTP events"
    :ventas.pages.admin.statistics/realtime-period "Period"

    :ventas.pages.admin.taxes/name "Name"
    :ventas.pages.admin.taxes/amount "Amount"
    :ventas.pages.admin.taxes/actions "Actions"
    :ventas.pages.admin.taxes/create "Create tax"
    :ventas.pages.admin.taxes/page "Taxes"

    :ventas.pages.admin.orders/user "User"
    :ventas.pages.admin.orders/page "Orders"
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
    :ventas.pages.admin.orders.edit/status-history "Status history"
    :ventas.pages.admin.orders.edit/nothing-yet "Nothing yet"
    :ventas.pages.admin.orders.edit/date "Date"

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
    :ventas.pages.admin.activity-log/page "Activity log"

    :ventas.plugins.stripe.admin/stripe.public-key "Public key"
    :ventas.plugins.stripe.admin/stripe.private-key "Private key"
    :ventas.plugins.stripe.frontend/card-number "Card number"
    :ventas.plugins.stripe.frontend/expiration-date "Expiration date"
    :ventas.plugins.stripe.frontend/cvc "CVC"
    :ventas.plugins.stripe.frontend/pay "Pay"
    :ventas.plugins.stripe.frontend/postal-code "Postal code"

    :ventas.session/unregistered-error "You need to be a registered user to do that"

    :ventas.utils.formatting/percentage "%"
    :ventas.utils.formatting/amount ""

    :ventas.utils.validation/length-error (fn [{:keys [min max]}]
                                            (cond
                                              (and min max) (str "Length should be higher than " min " and lower than " max)
                                              min (str "Length should be higher than " min)
                                              max (str "Length should be lower than " max)))
    :ventas.utils.validation/email-error "Invalid email address"
    :ventas.utils.validation/required-error "This field is required"

    :discount.amount.kind/amount "Amount"
    :discount.amount.kind/percentage "Percentage"

    :image-size.algorithm/always-resize "Always resize"
    :image-size.algorithm/crop-and-resize "Crop and resize"
    :image-size.algorithm/resize-only-if-over-maximum "Resize only if over maximum"

    :user.role/administrator "Administrator"
    :user.role/user "User"

    :user.status/inactive "Inactive"
    :user.status/pending "Pending"
    :user.status/active "Active"
    :user.status/cancelled "Cancelled"
    :user.status/unregistered "Unregistered"

    :order.status/draft "Draft"
    :order.status/acknowledged "Acknowledged"
    :order.status/paid "Paid"
    :order.status/ready "Ready"
    :order.status/shipped "Shipped"
    :order.status/unpaid "Unpaid"
    :order.status/rejected "Rejected"
    :order.status/cancelled "Cancelled"

    :entity.create "Creation"
    :entity.delete "Deletion"
    :entity.update "Update"

    :schema.type/category "Category"
    :schema.type/user "User"
    :schema.type/product "Product"
    :schema.type/brand "Brand"

    :shipping-method.pricing/price "Price"
    :shipping-method.pricing/weight "Weight"

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
  (apply @translation-fn :en_US kw args))

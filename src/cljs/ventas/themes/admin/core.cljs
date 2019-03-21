(ns ventas.themes.admin.core
  (:require
   [ventas.core]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.activity-log]
   [ventas.routes :as routes]
   [ventas.themes.admin.configuration.email]
   [ventas.themes.admin.configuration.general]
   [ventas.themes.admin.configuration.image-sizes]
   [ventas.themes.admin.configuration]
   [ventas.themes.admin.customization]
   [ventas.themes.admin.dashboard :as dashboard]
   [ventas.themes.admin.orders.edit]
   [ventas.themes.admin.orders]
   [ventas.themes.admin.payment-methods]
   [ventas.themes.admin.plugins]
   [ventas.themes.admin.products.discounts.edit]
   [ventas.themes.admin.products.discounts]
   [ventas.themes.admin.products.edit]
   [ventas.themes.admin.products]
   [ventas.themes.admin.shipping-methods]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.themes.admin.taxes]
   [ventas.themes.admin.users.edit]
   [ventas.themes.admin.users]
   [ventas.i18n :as i18n]))

(rf/reg-event-fx
 ::handle-route-change
 (fn [{:keys [db]} [_ [_ handler]]]
   (if (and (str/starts-with? (name handler) "admin")
            (not (get-in db [::state :init-done?])))
     {:dispatch [::admin.skeleton/init]
      :db (assoc-in db [::state :init-done?] true)}
     {})))

(rf/reg-event-fx
 ::listen-to-route-change
 (fn [_ _]
   {:forward-events {:register ::route-listener
                     :events #{::routes/set}
                     :dispatch-to [::handle-route-change]}}))

(rf/dispatch [::listen-to-route-change])

(routes/define-route!
 :admin
 {:name ::dashboard/page
  :url "admin"
  :component dashboard/page
  :init-fx [::dashboard/init]})

(i18n/register-translations!
 {:en_US
  {:ventas.themes.admin.configuration.email/email.from "From address"
   :ventas.themes.admin.configuration.email/email.encryption.enabled "Enable encryption?"
   :ventas.themes.admin.configuration.email/email.encryption.type "Encryption type"
   :ventas.themes.admin.configuration.email/email.smtp.enabled "Use SMTP server?"
   :ventas.themes.admin.configuration.email/email.smtp.host "SMTP host"
   :ventas.themes.admin.configuration.email/email.smtp.port "SMTP port"
   :ventas.themes.admin.configuration.email/email.smtp.user "SMTP user"
   :ventas.themes.admin.configuration.email/email.smtp.password "SMTP password"
   :ventas.themes.admin.configuration.email/submit "Submit"
   :ventas.themes.admin.configuration.email/ssl "SSL"
   :ventas.themes.admin.configuration.email/tls "TLS"
   :ventas.themes.admin.configuration.email/saved "Saved!"
   :ventas.themes.admin.configuration.email/page "Email configuration"

   :ventas.themes.admin.configuration.general/culture "Default culture"
   :ventas.themes.admin.configuration.general/page "General configuration"
   :ventas.themes.admin.configuration.general/submit "Submit"


   :ventas.themes.admin.configuration.image-sizes/page "Image sizes"
   :ventas.themes.admin.configuration.image-sizes/actions "Actions"
   :ventas.themes.admin.configuration.image-sizes/create "Create image size"
   :ventas.themes.admin.configuration.image-sizes/width "Width"
   :ventas.themes.admin.configuration.image-sizes/height "Height"
   :ventas.themes.admin.configuration.image-sizes/algorithm "Algorithm"
   :ventas.themes.admin.configuration.image-sizes/keyword "ID"

   :ventas.themes.admin.configuration.image-sizes.edit/algorithm "Algorithm"
   :ventas.themes.admin.configuration.image-sizes.edit/name "Name"
   :ventas.themes.admin.configuration.image-sizes.edit/keyword "Keyword"
   :ventas.themes.admin.configuration.image-sizes.edit/width "Width"
   :ventas.themes.admin.configuration.image-sizes.edit/height "Height"
   :ventas.themes.admin.configuration.image-sizes.edit/entities "Entities"
   :ventas.themes.admin.configuration.image-sizes.edit/amount "Amount"
   :ventas.themes.admin.configuration.image-sizes.edit/submit "Submit"
   :ventas.themes.admin.configuration.image-sizes.edit/saved-notification "Saved!"

   :ventas.themes.admin.customization.customize/page "Customize"
   :ventas.themes.admin.customization.customize/name "Name"
   :ventas.themes.admin.customization.customize/logo "Logo"
   :ventas.themes.admin.customization.customize/header-image "Header image"
   :ventas.themes.admin.customization.customize/background-color "Background color"
   :ventas.themes.admin.customization.customize/foreground-color "Foreground color"
   :ventas.themes.admin.customization.customize/product-listing-mode "Product listing mode"
   :ventas.themes.admin.customization.customize/font-family "Font family"
   :ventas.themes.admin.customization.customize/save "Save"
   :ventas.themes.admin.customization.customize/back "Back"

   :ventas.themes.admin.customization.menus/name "Name"
   :ventas.themes.admin.customization.menus/items "Menu items"
   :ventas.themes.admin.customization.menus/page "Menus"

   :ventas.themes.admin.customization.menus.edit/page "Menu"
   :ventas.themes.admin.customization.menus.edit/name "Name"
   :ventas.themes.admin.customization.menus.edit/link "Link"
   :ventas.themes.admin.customization.menus.edit/add "Add"
   :ventas.themes.admin.customization.menus.edit/menu-item "Menu item"
   :ventas.themes.admin.customization.menus.edit/menu-items "Menu items"
   :ventas.themes.admin.customization.menus.edit/add-menu-item "Add menu item"
   :ventas.themes.admin.customization.menus.edit/save "Save"
   :ventas.themes.admin.customization.menus.edit/menu "Menu"


   :ventas.themes.admin.dashboard/traffic-statistics "Traffic statistics"
   :ventas.themes.admin.dashboard/latest-users "Latest users"
   :ventas.themes.admin.dashboard/pending-orders "Pending orders"
   :ventas.themes.admin.dashboard/unread-messages "Unread messages"
   :ventas.themes.admin.dashboard/no-name "(No name)"
   :ventas.themes.admin.dashboard/page "Dashboard"
   :ventas.themes.admin.dashboard/statistics-disabled "Statistics are disabled"

   :ventas.themes.admin.products/create-product "Create product"
   :ventas.themes.admin.products/name "Name"
   :ventas.themes.admin.products/price "Price"
   :ventas.themes.admin.products/actions "Actions"
   :ventas.themes.admin.products/no-products "No products yet"

   :ventas.themes.admin/page "Administration"

   :ventas.themes.admin.users/page "Users"

   :ventas.themes.admin.users.edit/page "Edit user"
   :ventas.themes.admin.users.edit/user-saved-notification "User saved"

   :ventas.themes.admin/nothing-here "(Empty page)"

   :ventas.themes.admin.plugins/name "Name"
   :ventas.themes.admin.plugins/page "Plugins"
   :ventas.themes.admin.plugins/version "Version"

   :ventas.themes.admin.products/page "Products"

   :ventas.themes.admin.skeleton/discounts "Discounts"

   :ventas.themes.admin.shipping-methods/create "Create"
   :ventas.themes.admin.shipping-methods/name "Name"
   :ventas.themes.admin.shipping-methods/page "Shipping methods"
   :ventas.themes.admin.shipping-methods/actions "Actions"

   :ventas.themes.admin.shipping-methods.edit/page "Edit shipping method"
   :ventas.themes.admin.shipping-methods.edit/name "Name"
   :ventas.themes.admin.shipping-methods.edit/submit "Submit"
   :ventas.themes.admin.shipping-methods.edit/pricing "Pricing"
   :ventas.themes.admin.shipping-methods.edit/default? "Default?"
   :ventas.themes.admin.shipping-methods.edit/manipulation-fee "Manipulation fee"
   :ventas.themes.admin.shipping-methods.edit/logo "Logo"
   :ventas.themes.admin.shipping-methods.edit/shipping-method "Shipping method"

   :ventas.themes.admin.payment-methods/page "Billing"

   :ventas.themes.admin.products.discounts/name "Name"
   :ventas.themes.admin.products.discounts/page "Discounts"
   :ventas.themes.admin.products.discounts/code "Code"
   :ventas.themes.admin.products.discounts/amount "Amount"
   :ventas.themes.admin.products.discounts/actions "Actions"
   :ventas.themes.admin.products.discounts/create "Create"

   :ventas.themes.admin.products.discounts.edit/name "Name"
   :ventas.themes.admin.products.discounts.edit/page "Edit discount"
   :ventas.themes.admin.products.discounts.edit/code "Code"
   :ventas.themes.admin.products.discounts.edit/active? "Active"
   :ventas.themes.admin.products.discounts.edit/max-uses-per-customer "Max uses per customer"
   :ventas.themes.admin.products.discounts.edit/max-uses "Max uses"
   :ventas.themes.admin.products.discounts.edit/free-shipping? "Free shipping"
   :ventas.themes.admin.products.discounts.edit/product "Product"
   :ventas.themes.admin.products.discounts.edit/amount "Amount"
   :ventas.themes.admin.products.discounts.edit/amount.tax-included? "Tax included?"
   :ventas.themes.admin.products.discounts.edit/amount.kind "Kind"
   :ventas.themes.admin.products.discounts.edit/submit "Submit"

   :ventas.themes.admin.products.edit/active "Active"
   :ventas.themes.admin.products.edit/brand "Brand"
   :ventas.themes.admin.products.edit/categories "Categories"
   :ventas.themes.admin.products.edit/description "Description"
   :ventas.themes.admin.products.edit/ean13 "EAN13"
   :ventas.themes.admin.products.edit/variation-terms "Variation terms"
   :ventas.themes.admin.products.edit/terms "Terms"
   :ventas.themes.admin.products.edit/images "Images"
   :ventas.themes.admin.products.edit/name "Name"
   :ventas.themes.admin.products.edit/page "Edit product"
   :ventas.themes.admin.products.edit/price "Price"
   :ventas.themes.admin.products.edit/product-saved-notification "Product saved"
   :ventas.themes.admin.products.edit/reference "Reference"
   :ventas.themes.admin.products.edit/send "Send"
   :ventas.themes.admin.products.edit/tags "Tags"
   :ventas.themes.admin.products.edit/tax "Tax"

   :ventas.themes.admin.skeleton/email "Email"
   :ventas.themes.admin.skeleton/dashboard "Dashboard"
   :ventas.themes.admin.skeleton/home "The store"
   :ventas.themes.admin.skeleton/password "Password"
   :ventas.themes.admin.skeleton/login "Login"
   :ventas.themes.admin.skeleton/logout "Logout"
   :ventas.themes.admin.skeleton/activity-log "Activity log"
   :ventas.themes.admin.skeleton/administration "Administration"
   :ventas.themes.admin.skeleton/configuration "Configuration"
   :ventas.themes.admin.skeleton/image-sizes "Image sizes"
   :ventas.themes.admin.skeleton/nothing-here "Nothing here"
   :ventas.themes.admin.skeleton/plugins "Plugins"
   :ventas.themes.admin.skeleton/products "Products"
   :ventas.themes.admin.skeleton/users "Users"
   :ventas.themes.admin.skeleton/taxes "Taxes"
   :ventas.themes.admin.skeleton/orders "Orders"
   :ventas.themes.admin.skeleton/payment-methods "Billing"
   :ventas.themes.admin.skeleton/shipping-methods "Shipping"
   :ventas.themes.admin.skeleton/customization "Customization"
   :ventas.themes.admin.skeleton/configuration.general "Configuration"

   :ventas.themes.admin.taxes/name "Name"
   :ventas.themes.admin.taxes/amount "Amount"
   :ventas.themes.admin.taxes/page "Taxes"

   :ventas.themes.admin.orders/user "User"
   :ventas.themes.admin.orders/page "Orders"
   :ventas.themes.admin.orders/status "Status"
   :ventas.themes.admin.orders/amount "Amount"
   :ventas.themes.admin.orders/actions "Actions"
   :ventas.themes.admin.orders/create "Create order"

   :ventas.themes.admin.orders.edit/user "User"
   :ventas.themes.admin.orders.edit/status "Status"
   :ventas.themes.admin.orders.edit/payment-reference "Payment reference"
   :ventas.themes.admin.orders.edit/submit "Submit"
   :ventas.themes.admin.orders.edit/order "Order"
   :ventas.themes.admin.orders.edit/billing "Billing"
   :ventas.themes.admin.orders.edit/payment-method "Payment method"
   :ventas.themes.admin.orders.edit/billing-address "Payment address"
   :ventas.themes.admin.orders.edit/shipping "Shipping"
   :ventas.themes.admin.orders.edit/shipping-method "Shipping method"
   :ventas.themes.admin.orders.edit/shipping-address "Shipping address"
   :ventas.themes.admin.orders.edit/shipping-comments "Comments"
   :ventas.themes.admin.orders.edit/lines "Order lines"
   :ventas.themes.admin.orders.edit/image "Image"
   :ventas.themes.admin.orders.edit/name "Name"
   :ventas.themes.admin.orders.edit/price "Price"
   :ventas.themes.admin.orders.edit/quantity "Quantity"
   :ventas.themes.admin.orders.edit/total "Total"
   :ventas.themes.admin.orders.edit/status-history "Status history"
   :ventas.themes.admin.orders.edit/nothing-yet "Nothing yet"
   :ventas.themes.admin.orders.edit/date "Date"

   :ventas.themes.admin.taxes.edit/amount "Amount"
   :ventas.themes.admin.taxes.edit/page "Taxes"
   :ventas.themes.admin.taxes.edit/name "Name"
   :ventas.themes.admin.taxes.edit/kind "Type"
   :ventas.themes.admin.taxes.edit/submit "Submit"

   :ventas.themes.admin.users/no-items "No items to show"
   :ventas.themes.admin.users/new-user "Create"
   :ventas.themes.admin.users/name "Name"
   :ventas.themes.admin.users/email "Email"
   :ventas.themes.admin.users/actions "Actions"

   :ventas.themes.admin.users.edit/first-name "First name"
   :ventas.themes.admin.users.edit/last-name "Last name"
   :ventas.themes.admin.users.edit/company "Company"
   :ventas.themes.admin.users.edit/phone "Phone"
   :ventas.themes.admin.users.edit/email "Email"
   :ventas.themes.admin.users.edit/roles "Roles"
   :ventas.themes.admin.users.edit/submit "Submit"
   :ventas.themes.admin.users.edit/culture "Culture"
   :ventas.themes.admin.users.edit/status "Status"

   :ventas.themes.admin.activity-log/whats-the-activity-log "In the activity log you can see everything that happens in your store. Filter the messages by category to find the information you want."
   :ventas.themes.admin.activity-log/entity-id "ID"
   :ventas.themes.admin.activity-log/entity-type "Entity type"
   :ventas.themes.admin.activity-log/type "Event type"
   :ventas.themes.admin.activity-log/page "Activity log"}})
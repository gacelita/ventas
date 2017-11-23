(ns ventas.i18n
  (:require
   [tongue.core :as tongue]
   [ventas.utils.logging :refer [debug]]
   [ventas.utils.goog :as utils.goog]))

(def dicts
  {:en_US

   {:ventas.page/not-found "404"
    :ventas.pages.admin.configuration.image-sizes/actions "Actions"
    :ventas.pages.admin.configuration.image-sizes/create-image-size "Create image size"
    :ventas.pages.admin.configuration.image-sizes/width "Width"
    :ventas.pages.admin.configuration.image-sizes/height "Height"
    :ventas.pages.admin.configuration.image-sizes/algorithm "Algorithm"

    :ventas.pages.admin.products/create-product "Create product"
    :ventas.pages.admin.products/name "Name"
    :ventas.pages.admin.products/email "Email"
    :ventas.pages.admin.products/actions "Actions"
    :ventas.pages.admin.products/no-products "No products yet"

    :ventas.pages.admin/page "Administration"

    :ventas.pages.admin.users/page "Users"

    :ventas.pages.admin.users.edit/page "Edit user"
    :ventas.pages.admin.users.edit/user-saved-notification "Usuario guardado"

    :ventas.pages.admin/nothing-here "(Empty page)"

    :ventas.pages.admin.products/page "Products"
    :ventas.pages.admin.products.edit/active "Active"
    :ventas.pages.admin.products.edit/brand "Brand"
    :ventas.pages.admin.products.edit/description "Description"
    :ventas.pages.admin.products.edit/ean13 "EAN13"
    :ventas.pages.admin.products.edit/images "Images"
    :ventas.pages.admin.products.edit/name "Name"
    :ventas.pages.admin.products.edit/page "Edit product"
    :ventas.pages.admin.products.edit/price "Price"
    :ventas.pages.admin.products.edit/product-saved-notification "Producto guardado"
    :ventas.pages.admin.products.edit/reference "Reference"
    :ventas.pages.admin.products.edit/send "Send"
    :ventas.pages.admin.products.edit/tags "Tags"
    :ventas.pages.admin.products.edit/tax "Tax"
    :ventas.pages.admin.skeleton/activity-log "Activity log"
    :ventas.pages.admin.skeleton/administration "Administration"
    :ventas.pages.admin.skeleton/configuration "Configuration"
    :ventas.pages.admin.skeleton/image-sizes "Image sizes"
    :ventas.pages.admin.skeleton/nothing-here "Nothing here"
    :ventas.pages.admin.skeleton/plugins "Plugins"
    :ventas.pages.admin.skeleton/products "Products"
    :ventas.pages.admin.skeleton/users "Users"
    :ventas.pages.admin.skeleton/taxes "Taxes"
    :ventas.pages.admin.taxes/name "Name"
    :ventas.pages.admin.taxes/quantity "Quantity"
    :ventas.pages.admin.taxes/actions "Actions"
    :ventas.pages.admin.taxes/create-tax "Create tax"
    :ventas.pages.admin.taxes.edit/amount "Amount"
    :ventas.pages.admin.taxes.edit/name "Name"
    :ventas.pages.admin.taxes.edit/kind "Type"
    :ventas.pages.admin.taxes.edit/submit "Submit"
    :ventas.pages.admin.activity-log/whats-the-activity-log "In the activity log you can see everything that happens in your store. Filter the messages by category to find the information you want."

    :ventas.themes.clothing.components.header/my-cart "My cart"
    :ventas.themes.clothing.components.header/my-account "My account"
    :ventas.themes.clothing.components.header/logout "Logout"
    :ventas.themes.clothing.components.menu/home "Home"
    :ventas.themes.clothing.components.menu/woman "Woman"
    :ventas.themes.clothing.components.menu/man "Man"
    :ventas.themes.clothing.components.skeleton/cookies "Al navegar por nuestra tienda indicas que estás de acuerdo con nuestra política de cookies."

    :ventas.themes.clothing.pages.frontend/page "Home"
    :ventas.themes.clothing.pages.frontend.category/page "%s"
    :ventas.themes.clothing.pages.frontend.cart/cart "Cart"
    :ventas.themes.clothing.pages.frontend.cart/page "Cart"
    :ventas.themes.clothing.pages.frontend.cart/product "Product"
    :ventas.themes.clothing.pages.frontend.cart/description "Description"
    :ventas.themes.clothing.pages.frontend.cart/price "Price"
    :ventas.themes.clothing.pages.frontend.cart/quantity "Quantity"
    :ventas.themes.clothing.pages.frontend.cart/total "Total"

    :ventas.themes.clothing.pages.frontend.login/login "Login"
    :ventas.themes.clothing.pages.frontend.login/register "Register"
    :ventas.themes.clothing.pages.frontend.login/full-name "Full name"
    :ventas.themes.clothing.pages.frontend.login/email "Email"
    :ventas.themes.clothing.pages.frontend.login/password "Password"
    :ventas.themes.clothing.pages.frontend.login/page "Login"
    :ventas.themes.clothing.pages.frontend.login/forgot-password "Password forgotten?"
    :ventas.themes.clothing.pages.frontend.login/user-registered "Your account is ready. Welcome!"

    :ventas.themes.clothing.pages.frontend.profile/welcome "Welcome %s"
    :ventas.themes.clothing.pages.frontend.profile/personal-data "Personal data"
    :ventas.themes.clothing.pages.frontend.profile/personal-data-explanation "View and change your personal data"
    :ventas.themes.clothing.pages.frontend.profile/my-orders-explanation "See all the orders you've ever done"
    :ventas.themes.clothing.pages.frontend.profile/my-addresses-explanation "Create or change your addresses (work, home...)"
    :ventas.themes.clothing.pages.frontend.profile/page "My profile"

    :ventas.themes.clothing.pages.frontend.profile/my-addresses "My addresses"
    :ventas.themes.clothing.pages.frontend.profile/my-orders "My orders"
    :ventas.themes.clothing.pages.frontend.profile/my-account "My account"

    :ventas.themes.clothing.pages.frontend.profile.skeleton/my-profile "My profile"
    :ventas.themes.clothing.pages.frontend.profile.skeleton/my-addresses "My addresses"
    :ventas.themes.clothing.pages.frontend.profile.skeleton/my-orders "My orders"
    :ventas.themes.clothing.pages.frontend.profile.skeleton/my-account "My account"
    :ventas.themes.clothing.pages.frontend.profile.skeleton/logout "Logout"
    :ventas.themes.clothing.pages.frontend.profile.account/length-error "The maximum length is %s"
    :ventas.themes.clothing.pages.frontend.profile.account/first-name "First name"
    :ventas.themes.clothing.pages.frontend.profile.account/last-name "Last name"
    :ventas.themes.clothing.pages.frontend.profile.account/company "Company"
    :ventas.themes.clothing.pages.frontend.profile.account/address "Address"
    :ventas.themes.clothing.pages.frontend.profile.account/address-second-line "Address (second line)"
    :ventas.themes.clothing.pages.frontend.profile.account/city "City"
    :ventas.themes.clothing.pages.frontend.profile.account/email "Email"
    :ventas.themes.clothing.pages.frontend.profile.account/privacy-policy-text "I've read and I accept the"
    :ventas.themes.clothing.pages.frontend.profile.account/privacy-policy "privacy policy"
    :ventas.themes.clothing.pages.frontend.profile.account/state "State"
    :ventas.themes.clothing.pages.frontend.profile.account/phone "Phone"
    :ventas.themes.clothing.pages.frontend.profile.account/zip-code "ZIP code"
    :ventas.themes.clothing.pages.frontend.profile.account/page "My account"
    :ventas.themes.clothing.pages.frontend.profile.account/save "Save"


    :ventas.utils.formatting/percentage "%"
    :ventas.utils.formatting/amount ""
    :ventas.utils.formatting/euro " €"

    :user.role/administrator "Administrator"
    :user.role/user "User"
    :tax.kind/amount "Amount"
    :tax.kind/percentage "Percentage"}

   :tongue/fallback :en_US})

(def ^:private translation-fn (tongue/build-translate dicts))

(defn i18n [kw & args]
  (apply utils.goog/format (translation-fn :en_US kw) args))
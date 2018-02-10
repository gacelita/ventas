(ns ventas.themes.clothing.core
  "See the docstring in the server version of this file"
  (:require
   [ventas.core]
   [ventas.i18n :as i18n]
   [ventas.themes.clothing.pages.frontend]))

(i18n/register-translations!
 {:en_US
  {:ventas.themes.clothing.components.header/my-cart "My cart"
   :ventas.themes.clothing.components.header/my-account "My account"
   :ventas.themes.clothing.components.header/logout "Logout"
   :ventas.themes.clothing.components.header/search "Search"
   :ventas.themes.clothing.components.header/product "Product"
   :ventas.themes.clothing.components.header/category "Category"

   :ventas.themes.clothing.components.menu/home "Home"
   :ventas.themes.clothing.components.menu/women "Women"
   :ventas.themes.clothing.components.menu/men "Men"

   :ventas.themes.clothing.components.footer/footer-text "This is the example Ventas theme."
   :ventas.themes.clothing.components.footer/footer-subtext "Here you can add custom text."
   :ventas.themes.clothing.components.footer/links "Links"
   :ventas.themes.clothing.components.footer/privacy-policy "Privacy policy"
   :ventas.themes.clothing.components.footer/contact "Contact information"

   :ventas.themes.clothing.components.skeleton/cookies "We use cookies on this site to enhance your user experience."

   :ventas.themes.clothing.pages.frontend/page "Home"
   :ventas.themes.clothing.pages.frontend/suggestions-of-the-week "Suggestions of the week"
   :ventas.themes.clothing.pages.frontend/recently-added "Recently added"

   :ventas.themes.clothing.pages.frontend.category/page "%s"
   :ventas.themes.clothing.pages.frontend.category/search "Search"
   :ventas.themes.clothing.pages.frontend.category/highest-price "Highest price"
   :ventas.themes.clothing.pages.frontend.category/lowest-price "Lowest price"

   :ventas.themes.clothing.pages.frontend.cart/cart "Cart"
   :ventas.themes.clothing.pages.frontend.cart/page "Cart"
   :ventas.themes.clothing.pages.frontend.cart/product "Product"
   :ventas.themes.clothing.pages.frontend.cart/description "Description"
   :ventas.themes.clothing.pages.frontend.cart/price "Price"
   :ventas.themes.clothing.pages.frontend.cart/quantity "Quantity"
   :ventas.themes.clothing.pages.frontend.cart/total "Total"
   :ventas.themes.clothing.pages.frontend.cart/add-voucher "Add voucher"
   :ventas.themes.clothing.pages.frontend.cart/add "Add"
   :ventas.themes.clothing.pages.frontend.cart/subtotal "Subtotal"
   :ventas.themes.clothing.pages.frontend.cart/shipping "Shipping"
   :ventas.themes.clothing.pages.frontend.cart/payment "Payment"
   :ventas.themes.clothing.pages.frontend.cart/checkout "Checkout"
   :ventas.themes.clothing.pages.frontend.cart/free "Free"
   :ventas.themes.clothing.pages.frontend.cart/no-items "Your cart is empty"

   :ventas.themes.clothing.pages.frontend.login/login "Login"
   :ventas.themes.clothing.pages.frontend.login/register "Register"
   :ventas.themes.clothing.pages.frontend.login/full-name "Full name"
   :ventas.themes.clothing.pages.frontend.login/email "Email"
   :ventas.themes.clothing.pages.frontend.login/password "Password"
   :ventas.themes.clothing.pages.frontend.login/page "Login"
   :ventas.themes.clothing.pages.frontend.login/forgot-password "Password forgotten?"
   :ventas.themes.clothing.pages.frontend.login/user-registered "Your account is ready. Welcome!"

   :ventas.themes.clothing.pages.frontend.privacy-policy/privacy-policy "Privacy policy"
   :ventas.themes.clothing.pages.frontend.privacy-policy/privacy-policy-text "Just some example text"

   :ventas.themes.clothing.pages.frontend.product/page "Product"
   :ventas.themes.clothing.pages.frontend.product/add-to-cart "Add to cart"
   :ventas.themes.clothing.pages.frontend.product/product-details "Product details"

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
   :ventas.themes.clothing.pages.frontend.profile.account/email "Email"
   :ventas.themes.clothing.pages.frontend.profile.account/privacy-policy-text "I've read and I accept the"
   :ventas.themes.clothing.pages.frontend.profile.account/privacy-policy "privacy policy"
   :ventas.themes.clothing.pages.frontend.profile.account/page "My account"
   :ventas.themes.clothing.pages.frontend.profile.account/save "Save"
   :ventas.themes.clothing.pages.frontend.profile.account/phone "Phone"

   :ventas.themes.clothing.pages.frontend.profile.addresses/address "Address"
   :ventas.themes.clothing.pages.frontend.profile.addresses/address-second-line "Address (second line)"
   :ventas.themes.clothing.pages.frontend.profile.addresses/city "City"
   :ventas.themes.clothing.pages.frontend.profile.addresses/state "State"
   :ventas.themes.clothing.pages.frontend.profile.addresses/phone "Phone"
   :ventas.themes.clothing.pages.frontend.profile.addresses/zip-code "ZIP code"
   :ventas.themes.clothing.pages.frontend.profile.addresses/first-name "First name"
   :ventas.themes.clothing.pages.frontend.profile.addresses/last-name "Last name"
   :ventas.themes.clothing.pages.frontend.profile.addresses/save "Save"
   :ventas.themes.clothing.pages.frontend.profile.addresses/company "Company"
   :ventas.themes.clothing.pages.frontend.profile.addresses/new-address "New address"
   :ventas.themes.clothing.pages.frontend.profile.addresses/my-addresses "My addresses"
   :ventas.themes.clothing.pages.frontend.profile.addresses/edit "Edit"
   :ventas.themes.clothing.pages.frontend.profile.addresses/remove "Remove"
   :ventas.themes.clothing.pages.frontend.profile.addresses/page "My addresses"
   :ventas.themes.clothing.pages.frontend.profile.addresses/address-removed "Address removed!"
   :ventas.themes.clothing.pages.frontend.profile.addresses/editing-address "Editing an address"
   :ventas.themes.clothing.pages.frontend.profile.addresses/cancel "Cancel"
   :ventas.themes.clothing.pages.frontend.profile.addresses/address-saved "Address saved!"

   :ventas.themes.clothing.pages.frontend.profile.orders/page "My orders"

   :ventas.themes.clothing.components.preheader/support-and-orders "Support and orders:"
   :ventas.themes.clothing.components.preheader/schedule "Schedule:"
   :ventas.themes.clothing.components.preheader/schedule-info "Monday to Friday, 9am - 5pm"}})

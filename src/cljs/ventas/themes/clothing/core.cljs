(ns ventas.themes.clothing.core
  "See the docstring in the server version of this file"
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.components.cookies :as cookies]
   [ventas.core]
   [ventas.events :as events]
   [ventas.i18n :as i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.pages.frontend]))

(defmulti handle-event (fn [name] name))

(defmethod handle-event ::events/session.start [_ {:keys [user]}]
  (when (and (not (= (:status user) :user.status/unregistered))
             (= (routes/handler) :frontend.login))
    {:go-to [:frontend.profile]}))

(defmethod handle-event :default [_]
  {})

(rf/reg-event-fx
 ::handle-event
 (fn [_ [_ evt]]
   (or (apply handle-event evt)
       {})))

(rf/reg-event-fx
 ::listen-to-session-start
 (fn [_ _]
   {:forward-events {:register ::session-listener
                     :events #{::events/session.start}
                     :dispatch-to [::handle-event]}}))

(rf/reg-event-fx
 ::listen-to-route-change
 (fn [_ _]
   {:forward-events {:register ::route-listener
                     :events #{::routes/set}
                     :dispatch-to [::handle-route-change]}}))

(rf/reg-event-fx
 ::handle-route-change
 (fn [{:keys [db]} [_ [_ handler]]]
   (if (and (str/starts-with? (name handler) "frontend")
            (not (get-in db [::state :init-done?])))
     {:dispatch-n [[::cookies/get-state-from-local-storage]
                   [::events/configuration.get #{:site.title}]]
      :db (assoc-in db [::state :init-done?] true)}
     {})))

(rf/dispatch [::listen-to-session-start])
(rf/dispatch [::listen-to-route-change])

(i18n/register-translations!
 {:en_US
  {:ventas.themes.clothing.components.address/address "Address"
   :ventas.themes.clothing.components.address/address-second-line "Address (second line)"
   :ventas.themes.clothing.components.address/city "City"
   :ventas.themes.clothing.components.address/state "State"
   :ventas.themes.clothing.components.address/country "Country"
   :ventas.themes.clothing.components.address/phone "Phone"
   :ventas.themes.clothing.components.address/zip "ZIP code"
   :ventas.themes.clothing.components.address/first-name "First name"
   :ventas.themes.clothing.components.address/last-name "Last name"
   :ventas.themes.clothing.components.address/company "Company"

   :ventas.themes.clothing.components.header/my-cart "Cart"
   :ventas.themes.clothing.components.header/my-favorites "Favorites"
   :ventas.themes.clothing.components.header/my-account "Profile"
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

   :ventas.themes.clothing.pages.frontend.category/page "{1}"
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

   :ventas.themes.clothing.pages.frontend.favorites/page "Favorites"
   :ventas.themes.clothing.pages.frontend.favorites/favorites "Favorites"

   :ventas.themes.clothing.pages.frontend.checkout/checkout "Checkout"
   :ventas.themes.clothing.pages.frontend.checkout/page "Checkout"
   :ventas.themes.clothing.pages.frontend.checkout/contact-information "Contact information"
   :ventas.themes.clothing.pages.frontend.checkout/already-registered "Already have an account?"
   :ventas.themes.clothing.pages.frontend.checkout/login "Login"
   :ventas.themes.clothing.pages.frontend.checkout/email "Email"
   :ventas.themes.clothing.pages.frontend.checkout/shipping-address "Shipping address"
   :ventas.themes.clothing.pages.frontend.checkout/shipping-method "Shipping method"
   :ventas.themes.clothing.pages.frontend.checkout/payment-method "Payment method"
   :ventas.themes.clothing.pages.frontend.checkout/order "Order"
   :ventas.themes.clothing.pages.frontend.checkout/new-address "New address"

   :ventas.themes.clothing.pages.frontend.checkout.success/page "Order placed"
   :ventas.themes.clothing.pages.frontend.checkout.success/thank-you "Thank you!"
   :ventas.themes.clothing.pages.frontend.checkout.success/order-placed "Your order has been placed. You should receive a confirmation email soon."

   :ventas.themes.clothing.pages.frontend.login/login "Login"
   :ventas.themes.clothing.pages.frontend.login/register "Register"
   :ventas.themes.clothing.pages.frontend.login/full-name "Full name"
   :ventas.themes.clothing.pages.frontend.login/email "Email"
   :ventas.themes.clothing.pages.frontend.login/password "Password"
   :ventas.themes.clothing.pages.frontend.login/page "Login"
   :ventas.themes.clothing.pages.frontend.login/forgot-password "Password forgotten?"
   :ventas.themes.clothing.pages.frontend.login/user-registered "Your account is ready. Welcome!"

   :ventas.themes.clothing.pages.frontend.privacy-policy/privacy-policy "Privacy policy"
   :ventas.themes.clothing.pages.frontend.privacy-policy/page "Privacy policy"
   :ventas.themes.clothing.pages.frontend.privacy-policy/privacy-policy-text "Just some example text"

   :ventas.themes.clothing.pages.frontend.product/page "Product"
   :ventas.themes.clothing.pages.frontend.product/add-to-cart "Add to cart"
   :ventas.themes.clothing.pages.frontend.product/product-details "Product details"
   :ventas.themes.clothing.pages.frontend.product/is-required "is required"
   :ventas.themes.clothing.pages.frontend.product/sibling-products "Similar products"

   :ventas.themes.clothing.pages.frontend.profile/welcome "Welcome {1}"
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
   :ventas.themes.clothing.pages.frontend.profile.account/length-error "The maximum length is {1}"
   :ventas.themes.clothing.pages.frontend.profile.account/first-name "First name"
   :ventas.themes.clothing.pages.frontend.profile.account/last-name "Last name"
   :ventas.themes.clothing.pages.frontend.profile.account/company "Company"
   :ventas.themes.clothing.pages.frontend.profile.account/email "Email"
   :ventas.themes.clothing.pages.frontend.profile.account/privacy-policy-text "I've read and I accept the"
   :ventas.themes.clothing.pages.frontend.profile.account/privacy-policy "privacy policy"
   :ventas.themes.clothing.pages.frontend.profile.account/page "My account"
   :ventas.themes.clothing.pages.frontend.profile.account/submit "Save"
   :ventas.themes.clothing.pages.frontend.profile.account/phone "Phone"
   :ventas.themes.clothing.pages.frontend.profile.account/password "Password"
   :ventas.themes.clothing.pages.frontend.profile.account/password-repeat "Repeat your password"

   :ventas.themes.clothing.pages.frontend.profile.addresses/save "Save"
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

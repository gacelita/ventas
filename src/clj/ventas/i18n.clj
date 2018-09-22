(ns ventas.i18n
  "Server-side tongue wrapper"
  (:require
   [tongue.core :as tongue]))

(def ^:private dicts
  {:en_US
   {:ventas.email/new-pending-order "New pending order"
    :ventas.i18n/test-value "Test value"

    :ventas.database.entity/database-not-migrated "The database needs to be migrated before doing this"
    :ventas.database/database-connection-error "Database connection error"
    :ventas.database/transact-exception "Database exception"
    :ventas.entities.configuration/access-denied (fn [{:keys [key]}]
                                                   (str "The current user is not allowed to read the " key " configuration key"))

    :ventas.email.templates.order-status-changed/heading (fn [status ref]
                                                           (case status
                                                             :order.status/unpaid (str "We've received your order #" ref ". We'll notify you when we receive your payment.")
                                                             :order.status/paid (str "We've received your order #" ref ". We'll begin preparing it soon.")
                                                             :order.status/acknowledged (str "We're preparing your order #" ref ". We'll notify you when it's shipped.")
                                                             :order.status/ready (str "Your order #" ref " is ready, and will be updated when we receive your payment.")
                                                             :order.status/shipped (str "Your order #" ref " has been shipped.")
                                                             :order.status/cancelled (str "You've cancelled your order #" ref ".")
                                                             :order.status/rejected (str "Your order #" ref " has been rejected.")))
    :ventas.email.templates.order-status-changed/subject (fn [status ref]
                                                           (str "Order #" ref " - "
                                                                (case status
                                                                  :order.status/unpaid "Unpaid"
                                                                  :order.status/paid "Paid"
                                                                  :order.status/acknowledged "Acknowledged"
                                                                  :order.status/ready "Ready"
                                                                  :order.status/shipped "Shipped"
                                                                  :order.status/cancelled "Cancelled"
                                                                  :order.status/rejected "Rejected")))
    :ventas.email.templates.order-status-changed/product "Product"
    :ventas.email.templates.order-status-changed/quantity "Quantity"
    :ventas.email.templates.order-status-changed/amount "Amount"
    :ventas.email.templates.order-status-changed/total-amount "Total amount"
    :ventas.email.templates.order-status-changed/shipping-address "Shipping address"
    :ventas.email.templates.order-status-changed/go-to-orders "You can see your orders"
    :ventas.email.templates.order-status-changed/go-to-orders-link "here"

    :ventas.email.templates.user-registered/welcome (fn [title] (str "Welcome to " title "!"))
    :ventas.email.templates.user-registered/add-an-address "Now it's a good moment to add an address to your profile, so that it's available for your orders."
    :ventas.email.templates.user-registered/go-to-profile "Click here to go to your profile"

    :ventas.email.templates.password-forgotten/reset-your-password "Reset your password"

    :ventas.email.elements/hello (fn [name]
                                   (str "Hello, " name "!"))
    :ventas.payment-method/payment-method-not-found (fn [{:keys [method]}]
                                                      (str "Payment method not found: " method))
    :ventas.plugin/plugin-not-found (fn [{:keys [keyword]}]
                                      (str "Plugin not found: " keyword))
    :ventas.plugins.slider.core/slider-not-found (fn [{:keys [keyword]}]
                                                   (str "Slider not found: " keyword))
    :ventas.search/elasticsearch-error "Elasticsearch error"
    :ventas.server.api.admin/unauthorized "Unauthorized: you need to be an administrator to do this"
    :ventas.server.api.user/authentication-required "Authentication required: your identity is invalid or missing"
    :ventas.server.api.user/discount-not-found (fn [{:keys [code]}]
                                                 (str "Discount not found: " code))
    :ventas.server.api.user/entity-update-unauthorized (fn [{:keys [entity-type]}]
                                                         (str "Unauthorized: you need to be the owner of this " entity-type " entity to update it"))
    :ventas.server.api/category-not-found (fn [{:keys [category]}]
                                            (str "Category not found: " category))
    :ventas.server.api/entity-not-found (fn [{:keys [entity]}]
                                          (str "Entity not found: " entity))
    :ventas.server.api/invalid-credentials "Invalid credentials"
    :ventas.server.api/invalid-ref (fn [{:keys [ref]}]
                                     (str "Invalid entity reference: " ref))
    :ventas.server.api/user-not-found "User not found"
    :ventas.server.ws/api-call-not-found (fn [{:keys [name]}]
                                           (str "The requested API call does not exist (" name ")"))
    :ventas.utils.images/file-not-found (fn [{:keys [path]}]
                                          (str "File not found: " path))
    :ventas.utils/spec-invalid "Validation error"}})

(def i18n (tongue/build-translate dicts))

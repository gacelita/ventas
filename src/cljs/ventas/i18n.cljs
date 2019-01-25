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

    :ventas.components.search-box/search "Search"
    :ventas.components.search-box/product "Product"
    :ventas.components.search-box/category "Category"
    :ventas.components.search-box/page "Page"

    :ventas.components.product-filters/category "Category"

    :ventas.components.notificator/saved "Saved!"
    :ventas.components.notificator/error "An error occurred."

    :ventas.events/session-started "Welcome!"

    :ventas.page/not-found "404"
    :ventas.page/not-implemented "Not implemented"
    :ventas.page/this-page-has-not-been-implemented #(str "This page (" % ") has not been implemented")

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

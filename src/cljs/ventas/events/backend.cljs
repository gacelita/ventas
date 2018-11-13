(ns ventas.events.backend
  "Events that wrap websocket requests to the backend.
   Using the :api-request effect directly is discouraged. Please wrap backend
   calls by creating events similar to the ones in this namespace."
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]))

(rf/reg-event-fx
 ::categories.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :categories.get} options)}))

(rf/reg-event-fx
 ::categories.options
 (fn [_ [_ options]]
    {:ws-request (merge {:name :categories.options} options)}))

(rf/reg-event-fx
 ::categories.list
 (fn [_ [_ options]]
   {:ws-request (common.utils/deep-merge
                 {:name :categories.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))

(rf/reg-event-fx
 ::configuration.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :configuration.get} options)}))

(rf/reg-event-fx
 ::layout.get
 (fn [_ [_ options]]
    {:ws-request (merge {:name :layout.get} options)}))

(rf/reg-event-fx
 ::entities.find
 (fn [_ [_ id options]]
   {:ws-request (merge {:name :entities.find
                        :params {:id id}}
                       options)}))

(rf/reg-event-fx
 ::image-sizes.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :image-sizes.list} options)}))

(rf/reg-event-fx
 ::products.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :products.get} options)}))

(rf/reg-event-fx
 ::products.list
 (fn [_ [_ options]]
   {:ws-request (common.utils/deep-merge
                 {:name :products.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))

(rf/reg-event-fx
 ::products.aggregations
 (fn [_ [_ options]]
   {:ws-request (merge {:name :products.aggregations} options)}))

(rf/reg-event-fx
 ::enums.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :enums.get} options)}))

(rf/reg-event-fx
 ::i18n.cultures.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :i18n.cultures.list} options)}))

(rf/reg-event-fx
 ::users.cart.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.cart.get} options)}))

(rf/reg-event-fx
 ::users.cart.shipping-methods
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.cart.shipping-methods} options)}))

(rf/reg-event-fx
 ::users.cart.payment-methods
 (fn [_ [_ options]]
    {:ws-request (merge {:name :users.cart.payment-methods} options)}))

(rf/reg-event-fx
 ::users.cart.add
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.cart.add} options)}))

(rf/reg-event-fx
 ::users.cart.remove
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.cart.remove} options)}))

(rf/reg-event-fx
 ::users.cart.set-quantity
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.cart.set-quantity} options)}))

(rf/reg-event-fx
 ::users.cart.order
 (fn [_ [_ options]]
    {:ws-request (merge {:name :users.cart.order} options)}))

(rf/reg-event-fx
 ::users.save
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.save} options)}))

(rf/reg-event-fx
 ::users.change-password
 (fn [_ [_ options]]
    {:ws-request (merge {:name :users.change-password} options)}))

(rf/reg-event-fx
 ::users.orders.list
 (fn [_ [_ options]]
    {:ws-request (merge {:name :users.orders.list} options)}))

(rf/reg-event-fx
 ::admin.configuration.set
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.configuration.set} options)}))

(rf/reg-event-fx
 ::admin.users.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.users.list} options)}))

(rf/reg-event-fx
 ::admin.search
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.search} options)}))

(rf/reg-event-fx
 ::admin.stats.realtime
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.stats.realtime} options)}))

(rf/reg-event-fx
 ::users.login
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.login} options)}))

(rf/reg-event-fx
 ::users.logout
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.logout} options)}))

(rf/reg-event-fx
 ::users.register
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.register} options)}))

(rf/reg-event-fx
 ::users.session
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.session} options)}))

(rf/reg-event-fx
 ::users.addresses
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.addresses} options)}))

(rf/reg-event-fx
 ::users.addresses.save
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.addresses.save} options)}))

(rf/reg-event-fx
 ::users.addresses.remove
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.addresses.remove} options)}))

(rf/reg-event-fx
 ::users.favorites.enumerate
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.favorites.enumerate} options)}))

(rf/reg-event-fx
 ::users.favorites.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.favorites.list} options)}))

(rf/reg-event-fx
 ::users.favorites.add
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.favorites.add} options)}))

(rf/reg-event-fx
 ::users.favorites.remove
 (fn [_ [_ options]]
   {:ws-request (merge {:name :users.favorites.remove} options)}))

(rf/reg-event-fx
 ::shipping-methods.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :shipping-methods.list} options)}))

(rf/reg-event-fx
 ::search
 (fn [_ [_ options]]
   {:ws-request (merge {:name :search} options)}))

(rf/reg-event-fx
 ::states.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :states.list} options)}))

(rf/reg-event-fx
 ::countries.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :countries.list} options)}))

(rf/reg-event-fx
 ::admin.events.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.events.list} options)}))

(rf/reg-event-fx
 ::admin.entities.remove
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.entities.remove} options)}))

(rf/reg-event-fx
 ::admin.entities.find
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.entities.find} options)}))

(rf/reg-event-fx
 ::admin.entities.pull
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.entities.pull} options)}))

(rf/reg-event-fx
 ::admin.entities.find-serialize
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.entities.find-serialize} options)}))

(rf/reg-event-fx
 ::admin.entities.save
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.entities.save} options)}))

(rf/reg-event-fx
 ::admin.entities.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.entities.list} options)}))

(rf/reg-event-fx
 ::admin.i18ns.find
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.i18ns.find} options)}))

(rf/reg-event-fx
 ::admin.image-sizes.entities.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.image-sizes.entities.list} options)}))

(rf/reg-event-fx
 ::admin.plugins.list
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.plugins.list} options)}))

(rf/reg-event-fx
 ::admin.orders.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :admin.orders.get} options)}))

(rf/reg-event-fx
 ::admin.orders.list-pending
 (fn [_ [_ options]]
    {:ws-request (merge {:name :admin.orders.list-pending} options)}))

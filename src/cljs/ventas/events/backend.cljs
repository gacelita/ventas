(ns ventas.events.backend
  "Events that wrap websocket requests to the backend.
   Using the :api-request effect directly is discouraged. Please wrap backend
   calls by creating events similar to the ones in this namespace."
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]))

(rf/reg-event-fx
 ::brands.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :brands.list} options)}))


(rf/reg-event-fx
 ::categories.get
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :categories.get} options)}))

(rf/reg-event-fx
 ::categories.list
 (fn [cofx [_ options]]
   {:ws-request (common.utils/deep-merge
                 {:name :categories.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))


(rf/reg-event-fx
 ::configuration.get
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :configuration.get} options)}))


(rf/reg-event-fx
 ::entities.remove
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :entities.remove} options)}))

(rf/reg-event-fx
 ::entities.find
 (fn [cofx [_ id options]]
   {:ws-request (merge {:name :entities.find
                        :params {:id id}}
                       options)}))


(rf/reg-event-fx
 ::events.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :events.list} options)}))


(rf/reg-event-fx
 ::image-sizes.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :image-sizes.list} options)}))


(rf/reg-event-fx
 ::products.get
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :products.get} options)}))

(rf/reg-event-fx
 ::products.list
 (fn [cofx [_ options]]
   {:ws-request (common.utils/deep-merge
                 {:name :products.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))

(rf/reg-event-fx
  ::plugins.list
  (fn [cofx [_ options]]
    {:ws-request (merge {:name :plugins.list} options)}))

(rf/reg-event-fx
 ::products.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :products.save} options)}))

(rf/reg-event-fx
 ::products.aggregations
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :products.aggregations} options)}))


(rf/reg-event-fx
 ::enums.get
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :enums.get} options)}))

(rf/reg-event-fx
  ::i18n.cultures.list
  (fn [cofx [_ options]]
    {:ws-request (merge {:name :i18n.cultures.list} options)}))

(rf/reg-event-fx
 ::taxes.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :taxes.list} options)}))

(rf/reg-event-fx
 ::taxes.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :taxes.save} options)}))


(rf/reg-event-fx
 ::users.cart.get
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.cart.get} options)}))

(rf/reg-event-fx
 ::users.cart.add
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.cart.add} options)}))

(rf/reg-event-fx
 ::users.cart.remove
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.cart.remove} options)}))

(rf/reg-event-fx
 ::users.cart.set-quantity
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.cart.set-quantity} options)}))

(rf/reg-event-fx
 ::users.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.list} options)}))

(rf/reg-event-fx
 ::users.login
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.login} options)}))

(rf/reg-event-fx
 ::users.register
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.register} options)}))

(rf/reg-event-fx
 ::users.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.save} options)}))

(rf/reg-event-fx
 ::users.session
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.session} options)}))

(rf/reg-event-fx
 ::users.addresses
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.addresses} options)}))

(rf/reg-event-fx
 ::users.addresses.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.addresses.save} options)}))

(rf/reg-event-fx
 ::users.favorites.list
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.favorites.list} options)}))

(rf/reg-event-fx
 ::users.favorites.add
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.favorites.add} options)}))

(rf/reg-event-fx
 ::users.favorites.remove
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.favorites.add} options)}))


(rf/reg-event-fx
 ::states.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :states.list} options)}))
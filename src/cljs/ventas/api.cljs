(ns ventas.api
  (:require
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [debug]]
   [ventas.utils.ui :as utils.ui]
   [ventas.common.util :as common.util]
   [ventas.i18n :refer [i18n]]))

#_"
  Universal subscription and event.
  Use a more specific subscription or event as needed."

(rf/reg-sub
 :ventas/db
 (fn [db [_ where]]
   (get-in db where)))

(rf/reg-event-db
 :ventas/db
 (fn [db [_ where what]]
   (debug :ventas/db where what)
   (assoc-in db where what)))

#_"
  Using :ws-request directly is discouraged.
  Available API calls should be registered here, to have control of what
  API calls the client is using, and to add a level of indirection, for a possible
  future where we'll want to deprecate or alter in some way certain API calls."

(rf/reg-event-fx
 :api/users.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.list} options)}))

(rf/reg-event-fx
 :api/users.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.save} options)}))

(rf/reg-event-fx
 :api/products.list
 (fn [cofx [_ options]]
   {:ws-request (common.util/deep-merge
                 {:name :products.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))

(rf/reg-event-fx
 :api/products.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :products.save} options)}))

(rf/reg-event-fx
 :api/entities.remove
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :entities.remove} options)}))

(rf/reg-event-fx
 :api/entities.find
 (fn [cofx [_ id options]]
   {:ws-request (merge {:name :entities.find
                        :params {:id id}}
                       options)}))

(rf/reg-event-fx
 :api/reference.user.role
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :reference.user.role}
                       options)}))

(rf/reg-event-fx
 :api/brands.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :brands.list} options)}))

(rf/reg-event-fx
 :api/taxes.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :taxes.list} options)}))

(utils.ui/reg-kw-sub :reference.user.role)

(rf/reg-event-fx
 :ventas/reference.user.role
 (fn [cofx [_]]
   (rf/dispatch [:api/reference.user.role
                 {:success-fn
                  (fn [options]
                    (rf/dispatch [:ventas/db [:reference.user.role]
                                  (map (fn [option]
                                         {:text (i18n (keyword option)) :value option})
                                       options)]))}])))

(rf/reg-event-fx
 :ventas/entities.sync
 (fn [cofx [_ eid]]
   (rf/dispatch [:api/entities.find eid
                 {:sync true
                  :success-fn (fn [entity-data]
                                (rf/dispatch [:ventas/db [:entities eid] entity-data]))}])))

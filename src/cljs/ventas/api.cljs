(ns ventas.api
  (:require
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [debug]]
   [ventas.utils.ui :as utils.ui]
   [ventas.common.util :as common.util]))

(rf/reg-event-db
 :ventas.api/success
 (fn [db [_ where what]]
   (debug :ventas.api/success where what)
   (assoc-in db where what)))

(rf/reg-event-fx
  :api/users.list
  (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.list} options)}))

(rf/reg-event-fx
 :api/products.list
 (fn [cofx [_ options]]
   {:ws-request (common.util/deep-merge
                 {:name :products.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))

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

(utils.ui/reg-kw-sub :reference.user.role)

(rf/reg-event-fx
 :ventas/reference.user.role
 (fn [cofx [_]]
   (rf/dispatch [:api/reference.user.role
                 {:success-fn
                  (fn [options]
                    (let [options (map (fn [option] {:text option :value option}) options)]
                      (rf/dispatch [:ventas.api/success [:reference.user.role] options])))}])))
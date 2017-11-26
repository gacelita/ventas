(ns ventas.api
  (:require
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [debug]]
   [ventas.utils.ui :as utils.ui]
   [ventas.common.util :as common.util]
   [ventas.i18n :refer [i18n]]
   [ventas.utils.formatting :as formatting]))

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
 :api/brands.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :brands.list} options)}))

(rf/reg-event-fx
 :api/categories.list
 (fn [cofx [_ options]]
   {:ws-request (common.util/deep-merge
                 {:name :categories.list
                  :params {:pagination {:page 0 :items-per-page 5}}} options)}))

(rf/reg-event-fx
 :api/configuration.get
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :configuration.get} options)}))

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
 :api/events.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :events.list} options)}))

(rf/reg-event-fx
 :api/image-sizes.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :image-sizes.list} options)}))

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
 :api/products.aggregations
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :products.aggregations} options)}))

(rf/reg-event-fx
 :api/reference
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :reference}
                       options)}))

(rf/reg-event-fx
 :api/resources.get
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :resources.get} options)}))

(rf/reg-event-fx
 :api/taxes.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :taxes.list} options)}))

(rf/reg-event-fx
 :api/taxes.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :taxes.save} options)}))

(rf/reg-event-fx
 :api/users.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.list} options)}))

(rf/reg-event-fx
 :api/users.login
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.login} options)}))

(rf/reg-event-fx
 :api/users.register
 (fn [cofx [_ options]]
    {:ws-request (merge {:name :users.register} options)}))

(rf/reg-event-fx
 :api/users.save
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.save} options)}))

(rf/reg-event-fx
 :api/users.session
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.session} options)}))

(rf/reg-event-fx
 :api/users.addresses
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :users.addresses} options)}))

(rf/reg-event-fx
 :ventas/configuration.get
 (fn [cofx [_ key]]
   {:dispatch [:api/configuration.get
               {:params {:keyword key}
                :success-fn
                (fn [data]
                  (rf/dispatch [:ventas/db [:configuration key] data]))}]}))

(rf/reg-event-fx
 :ventas/reference
 (fn [cofx [_ type]]
   {:dispatch [:api/reference
               {:params {:type type}
                :success-fn
                (fn [options]
                  (rf/dispatch [:ventas/db [:reference type]
                                (map (fn [option]
                                       {:text (i18n (keyword option))
                                        :value option})
                                     options)]))}]}))

(rf/reg-event-fx
 :ventas/resources.get
 (fn [cofx [_ key]]
   {:dispatch [:api/resources.get
               {:params {:keyword key}
                :success-fn
                (fn [data]
                  (rf/dispatch [:ventas/db [:resources key] data]))}]}))

(rf/reg-event-fx
 :ventas/entities.sync
 (fn [cofx [_ eid]]
   {:dispatch [:api/entities.find eid
               {:sync true
                :success-fn (fn [entity-data]
                              (rf/dispatch [:ventas/db [:entities eid] entity-data]))}]}))

(rf/reg-event-fx
 :ventas/entities.remove
 (fn [cofx [_ eid]]
   {:dispatch [:api/entities.remove
               {:params {:id eid}
                :success-fn #(rf/dispatch [:ventas/entities.remove.next eid])}]}))

(rf/reg-event-db
 :ventas/entities.remove.next
 (fn [db [_ eid]]
   (update db :entities #(dissoc eid))))

(rf/reg-event-fx
 :ventas/session.start
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [db local-storage]} [_]]
   (let [token (:token local-storage)]
     (when (seq token)
       {:dispatch [:api/users.session
                   {:params {:token token}
                    :success-fn #(rf/dispatch [:ventas/db [:session] %])}]}))))

(rf/reg-event-fx
 :ventas/session.stop
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [local-storage db]}]
   {:db (dissoc db :session)
    :local-storage (dissoc local-storage :token)}))

(rf/reg-event-fx
 :ventas/image-sizes.list
 (fn [cofx [_ db-key]]
   {:dispatch [:api/image-sizes.list {:success-fn #(rf/dispatch [:ventas/db db-key %])}]}))

(rf/reg-event-fx
 :ventas/taxes.list
 (fn [cofx [_ db-key]]
   {:dispatch [:api/taxes.list {:success-fn #(rf/dispatch [:ventas/taxes.list.next db-key %])}]}))

(rf/reg-event-db
 :ventas/taxes.list.next
 (fn [db [_ db-key data]]
   (->> data
        (map #(assoc % :quantity (formatting/format-number
                                  (:amount %)
                                  (keyword "ventas.utils.formatting"
                                           (name (:kind %))))))
        (assoc-in db db-key))))

(rf/reg-event-fx
 :ventas/upload
 (fn [cofx [_ {:keys [success-fn file]}]]
   (let [fr (js/FileReader.)]
     (set! (.-onload fr) #(rf/dispatch [:effects/ws-upload-request
                                        {:name :upload
                                         :upload-key :bytes
                                         :upload-data (-> fr .-result)
                                         :success-fn success-fn}]))
     (.readAsArrayBuffer fr file))))
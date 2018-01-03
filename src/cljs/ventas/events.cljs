(ns ventas.events
  "Application-wide events"
  (:require
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [day8.re-frame.forward-events-fx]
   [ventas.utils.formatting :as formatting]
   [ventas.utils.logging :refer [debug]]
   [ventas.events.backend :as backend]))

(defn- normalize-where [where]
  (if (keyword? where)
    [where]
    where))

;; Universal subscription
(rf/reg-sub
 ::db
 (fn [db [_ where]]
   (get-in db (normalize-where where))))

;; Universal event
;; Same as with the subscription: use a more specific one if possible
(rf/reg-event-db
 ::db
 (fn [db [_ where what]]
   (let [where (normalize-where where)]
     (debug ::db where what)
     (assoc-in db where what))))

;; Same as ::db but accepts a function
(rf/reg-event-db
 ::db.update
 (fn [db [_ where what-fn]]
   {:pre [(ifn? what-fn)]}
   (let [where (normalize-where where)]
     (debug ::db.update where what-fn)
     (update-in db where what-fn))))

(rf/reg-event-fx
 ::configuration.get
 (fn [cofx [_ key]]
   {:dispatch [::backend/configuration.get
               {:params {:keyword key}
                :success
                (fn [data]
                  (rf/dispatch [::db [:configuration key] data]))}]}))

(rf/reg-event-fx
 ::enums.get
 (fn [cofx [_ type]]
   {:dispatch [::backend/enums.get
               {:params {:type type}
                :success
                (fn [options]
                  (rf/dispatch [::db [:enums type]
                                (map (fn [option]
                                       {:text (i18n (keyword option))
                                        :value option})
                                     options)]))}]}))

(rf/reg-event-fx
 ::entities.sync
 (fn [cofx [_ eid]]
   {:dispatch [::backend/entities.find eid
               {:sync true
                :success (fn [entity-data]
                           (rf/dispatch [::db [:entities eid] entity-data]))}]}))

(rf/reg-event-fx
 ::admin.entities.remove
 (fn [cofx [_ eid]]
   {:dispatch [::backend/admin.entities.remove
               {:params {:id eid}
                :success #(rf/dispatch [::admin.entities.remove.next eid])}]}))

(rf/reg-event-db
 ::admin.entities.remove.next
 (fn [db [_ eid]]
   (update db :entities #(dissoc eid))))

(rf/reg-event-fx
  ::i18n.cultures.list
  (fn [_ _]
    {:dispatch [::backend/i18n.cultures.list
                {:success ::i18n.cultures.list.next}]}))

(rf/reg-event-db
  ::i18n.cultures.list.next
  (fn [db [_ cultures]]
    (->> cultures
         (map (fn [culture]
                {:text (:name culture)
                 :value (:keyword culture)}))
         (assoc db :cultures))))

(rf/reg-event-fx
 ::session.start
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [db local-storage]} [_]]
   (let [token (:token local-storage)]
     (if (seq token)
       {:dispatch [::backend/users.session
                   {:params {:token token}
                    :success #(if (seq %)
                                (rf/dispatch [::session.start.next %])
                                (rf/dispatch [::session.start.error]))
                    :error ::session.start.error}]}
       {:dispatch [::session.start.error]}))))

(rf/reg-event-fx
 ::session.start.next
 (fn [cofx [_ data]]
   {:dispatch [::db [:session] data]}))

(rf/reg-event-fx
 ::session.start.error
 (fn [cofx [_]]
   {:dispatch [::db [:session] ::error]}))

(rf/reg-event-fx
 ::session.stop
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [local-storage db]}]
   {:db (dissoc db :session)
    :local-storage (dissoc local-storage :token)}))

(rf/reg-event-fx
  ::users.login
  (fn [_ [_ {:keys [email password]}]]
    {:dispatch [::backend/users.login
                {:params {:email email
                          :password password}
                 :success ::users.login.next}]}))

(rf/reg-event-fx
  ::users.login.next
  [(rf/inject-cofx :local-storage)]
  (fn [{:keys [db local-storage]} [_ {:keys [user token]}]]
    {:db (assoc db :session user)
     :local-storage (assoc local-storage :token token)
     :dispatch [:ventas.components.notificator/add
                {:message (i18n ::session-started)}]}))

(rf/reg-event-fx
  ::users.register
  (fn [_ [_ {:keys [name email password]}]]
    {:dispatch [::backend/users.register
                {:params {:name name
                          :email email
                          :password password}
                 :success [:ventas.components.notificator/add
                           {:message (i18n ::user-registered)}]}]}))

(rf/reg-event-fx
 ::users.addresses
 (fn [cofx [_ options]]
   {:forward-events {:register ::users.addresses.listener
                     :events #{::session.start.next}
                     :dispatch-to [::users.addresses.next options]}}))

(rf/reg-event-fx
 ::users.addresses.next
 (fn [cofx [_ options]]
   {:dispatch [::backend/users.addresses options]
    :forward-events {:unregister ::users.addresses.listener}}))

(rf/reg-event-fx
 ::users.favorites.list
 (fn [cofx [_ db-key]]
   {:dispatch [::backend/users.favorites.list
               {:success [::db :users.favorites]}]}))

(rf/reg-event-fx
 ::image-sizes.list
 (fn [cofx [_ db-key]]
   {:dispatch [::backend/image-sizes.list
               {:success #(rf/dispatch [::db db-key %])}]}))

(rf/reg-event-fx
 ::admin.taxes.list
 (fn [cofx [_ db-key]]
   {:dispatch [::backend/admin.taxes.list
               {:success #(rf/dispatch [::admin.taxes.list.next db-key %])}]}))

(rf/reg-event-db
 ::admin.taxes.list.next
 (fn [db [_ db-key data]]
   (->> data
        (map #(assoc % :quantity (str (formatting/format-number (:value %))
                                      " "
                                      (i18n (keyword "ventas.utils.formatting" (name (:kind %)))))))
        (assoc-in db db-key))))

(rf/reg-event-fx
 ::admin.upload
 (fn [cofx [_ {:keys [success file]}]]
   (let [fr (js/FileReader.)]
     (set! (.-onload fr) #(rf/dispatch [:effects/ws-upload-request
                                        {:name :admin.upload
                                         :upload-key :bytes
                                         :upload-data (-> fr .-result)
                                         :success success}]))
     (.readAsArrayBuffer fr file))))
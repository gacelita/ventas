(ns ventas.themes.admin.users
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.server.api :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.server.api.admin :as api.admin]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(def state-key ::state)

(rf/reg-event-fx
 ::remove
 (fn [_ [_ id]]
   {:dispatch [::api.admin/admin.entities.remove
               {:params {:id id}
                :success [::remove.next id]}]}))

(rf/reg-event-db
 ::remove.next
 (fn [db [_ id]]
   (update-in db
              [state-key :table :rows]
              (fn [users]
                (remove #(= (:id %) id)
                        users)))))

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(rf/dispatch [::remove id])}
    [base/icon {:name "remove"}]]])

(defn- footer []
  [base/button {:on-click #(routes/go-to :admin.users.edit :id 0)}
   (i18n ::new-user)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ state-path]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (table/get-state db state-path)]
     {:dispatch [::api.admin/admin.users.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items total]}]]
   (-> db
       (assoc-in [state-key :table :rows] items)
       (assoc-in [state-key :table :total] total))))

(defn- content []
  [:div.admin-users__table
   [table/table [state-key :table]]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::table/init [state-key :table]
               {:fetch-fx [::fetch]
                :columns [{:id :first-name
                           :label (i18n ::name)
                           :component (partial table/link-column :admin.users.edit :id :first-name)}
                          {:id :email
                           :label (i18n ::email)}
                          {:id :actions
                           :label (i18n ::actions)
                           :component action-column}]
                :footer footer}]}))

(routes/define-route!
  :admin.users
  {:name ::page
   :url "users"
   :component page
   :init-fx [::init]})

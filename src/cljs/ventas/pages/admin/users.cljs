(ns ventas.pages.admin.users
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as dt]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(def state-key ::users)

(rf/reg-event-fx
  ::remove
  (fn [cofx [_ id]]
    {:dispatch [::backend/admin.entities.remove
                {:params {:id id}
                 :success [::remove.next id]}]}))

(rf/reg-event-db
  ::remove.next
  (fn [db [_ id]]
    (update-in db
               [state-key :users]
               (fn [users]
                 (remove #(= (:id %) id)
                         users)))))

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(routes/go-to :admin.users.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true
                 :on-click #(rf/dispatch [::remove id])}
    [base/icon {:name "remove"}]]])

(defn- footer []
  [base/button {:on-click #(routes/go-to :admin.users.edit :id 0)}
   (i18n ::new-user)])

(rf/reg-event-fx
  ::fetch
  (fn [{:keys [db]} [_ {:keys [state-path]}]]
    (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
      {:dispatch [::backend/users.list
                  {:success ::fetch.next
                   :params {:pagination {:page page
                                         :items-per-page items-per-page}
                            :sorting {:direction sort-direction
                                      :field sort-column}}}]})))

(rf/reg-event-db
  ::fetch.next
  (fn [db [_ {:keys [items total]}]]
    (-> db
        (assoc-in [state-key :users] items)
        (assoc-in [state-key :table :total] total))))

(defn- first-name-column [{:keys [first-name id]}]
  [:a {:href (routes/path-for :admin.users.edit :id id)}
   first-name])

(defn- content []
  [:div.admin-users__table
   [table/table
    {:init-state {:sort-column :id}
     :state-path [state-key :table]
     :data-path [state-key :users]
     :fetch-fx ::fetch
     :columns [{:id :first-name
                :label (i18n ::name)
                :component first-name-column}
               {:id :email
                :label (i18n ::email)}
               {:id :actions
                :label (i18n ::actions)
                :component action-column}]
     :footer footer}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users__page
    [content action-column]]])

(routes/define-route!
 :admin.users
 {:name ::page
  :url "users"
  :component page})
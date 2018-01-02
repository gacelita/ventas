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
    {:dispatch [::backend/entities.remove
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

(defn- content []
  (rf/dispatch [::backend/users.list
                {:success #(rf/dispatch [::events/db [state-key :users] %])}])
  (fn []
    [:div.admin-users__table
     [table/table
      {:state-path [state-key :table]
       :data-path [state-key :users]
       :columns [{:id :first-name
                  :label (i18n ::name)}
                 {:id :email
                  :label (i18n ::email)}
                 {:id :actions
                  :label (i18n ::actions)
                  :component action-column}]
       :footer footer}]]))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users__page
    [content action-column]]])

(routes/define-route!
 :admin.users
 {:name ::page
  :url "users"
  :component page})
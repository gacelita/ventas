(ns ventas.pages.admin.users
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as dt]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.components.datatable :as datatable]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(def state-key ::users)

(defn- action-column [_ {:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(routes/go-to :admin.users.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true
                 :on-click #(rf/dispatch [::events/entities.remove id])}
    [base/icon {:name "remove"}]]])

(defn- users-datatable []
  (rf/dispatch [::backend/users.list
                {:success #(rf/dispatch [::events/db [state-key :users] %])}])
  (fn []
    [:div.admin-users__table
     [dt/datatable state-key [::events/db [state-key :users]]
      [{::dt/column-key [:first-name]
        ::dt/column-label (i18n ::name)}

       {::dt/column-key [:email]
        ::dt/column-label (i18n ::email)
        ::dt/sorting {::dt/enabled? true}}

       {::dt/column-key [:actions]
        ::dt/column-label (i18n ::actions)
        ::dt/render-fn action-column}]

      {::dt/pagination {::dt/enabled? true
                        ::dt/per-page 3}
       ::dt/table-classes ["ui" "table" "celled"]
       ::dt/empty-tbody-component (fn [] [:p (i18n ::no-items)])}]
     [:div.admin-users__pagination
      [datatable/pagination state-key [::events/db [state-key :users]]]]]))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users__page
    [users-datatable action-column]
    [base/button {:on-click #(routes/go-to :admin.users.edit :id 0)}
     (i18n ::new-user)]]])

(routes/define-route!
 :admin.users
 {:name ::page
  :url "users"
  :component page})
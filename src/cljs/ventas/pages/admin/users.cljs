(ns ventas.pages.admin.users
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [soda-ash.core :as sa]
            [ventas.util :refer [dispatch-page-event]]
            [ventas.utils.ui :as utils.ui]
            [ventas.pages.admin :as admin]
            [ventas.routes :as routes]
            [ventas.components.base :as base]))

(rf/reg-sub
 :admin.users
 (fn [db _]
   (-> db :admin-users)))

(defn users-datatable [action-column sub-key]
  (rf/dispatch [:api/users.list {:success-fn #(rf/dispatch [:ventas.api/success [sub-key] %])}])
  (utils.ui/reg-kw-sub sub-key)
  (fn [action-column sub-key]
    [dt/datatable (keyword (gensym "users")) [sub-key]
     [{::dt/column-key [:id] ::dt/column-label "#"
       ::dt/sorting {::dt/enabled? true}}

      {::dt/column-key [:name] ::dt/column-label "Name"}

      {::dt/column-key [:email] ::dt/column-label "Email"
       ::dt/sorting {::dt/enabled? true}}

      {::dt/column-key [:actions] ::dt/column-label "Actions"
       ::dt/render-fn action-column}]

     {::dt/pagination {::dt/enabled? true
                       ::dt/per-page 5}
      ::dt/table-classes ["ui" "table" "celled"]
      ::dt/empty-tbody-component (fn [] [:p "No users yet"])}]))

(defmethod pages :admin.users []
  [admin/skeleton
   (let [action-column
         (fn [_ row]
           [:div
            [base/button {:icon true :on-click #(routes/go-to :admin.users.edit :id (:id row))}
             [base/icon {:name "edit"}]]
            [base/button {:icon true :on-click #(rf/dispatch [:app/entity-remove {:id (:id row)} [:users]])}
             [base/icon {:name "remove"}]]])]
     [:div.admin-users__page
      [users-datatable action-column :users]
      [base/button {:onClick #(routes/go-to :admin.users.edit :id 0)} "Crear usuario"]])])
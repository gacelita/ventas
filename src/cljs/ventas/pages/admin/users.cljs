(ns ventas.pages.admin.users
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [re-frame-datatable.views :as dt.views]
   [ventas.page :refer [pages]]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.components.datatable :as datatable]
   [ventas.i18n :refer [i18n]]))

(defn users-datatable [action-column]
  (let [sub-key :users]
    (rf/dispatch [:api/users.list {:success #(rf/dispatch [:ventas/db [sub-key] %])}])
    (fn [action-column]
      (let [id (keyword (gensym "users"))]
        [:div
         [dt/datatable id [:ventas/db [sub-key]]
          [{::dt/column-key [:id]
            ::dt/column-label "#"
            ::dt/sorting {::dt/enabled? true}}

           {::dt/column-key [:name]
            ::dt/column-label "Name"}

           {::dt/column-key [:email]
            ::dt/column-label "Email"
            ::dt/sorting {::dt/enabled? true}}

           {::dt/column-key [:actions]
            ::dt/column-label "Actions"
            ::dt/render-fn action-column}]

          {::dt/pagination {::dt/enabled? true
                            ::dt/per-page 3}
           ::dt/table-classes ["ui" "table" "celled"]
           ::dt/empty-tbody-component (fn [] [:p "No users yet"])}]
         [:div.admin-users__pagination
          [datatable/pagination id [:ventas/db [sub-key]]]]]))))

(defn page []
  [admin.skeleton/skeleton
   (let [action-column
         (fn [_ row]
           [:div
            [base/button {:icon true :on-click #(routes/go-to :admin.users.edit :id (:id row))}
             [base/icon {:name "edit"}]]
            [base/button {:icon true :on-click #(rf/dispatch [:ventas/entities.remove (:id row)])}
             [base/icon {:name "remove"}]]])]
     [:div.admin__default-content.admin-users__page
      [users-datatable action-column]
      [base/button {:onClick #(routes/go-to :admin.users.edit :id 0)} "Crear usuario"]])])

(routes/define-route!
 :admin.users
 {:name (i18n ::page)
  :url "users"
  :component page})
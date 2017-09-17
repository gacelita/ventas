(ns ventas.pages.admin.users.edit
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [soda-ash.core :as sa]
            [ventas.utils.logging :refer [trace debug info warn error]]
            [ventas.components.base :as base]
            [ventas.page :refer [pages]]
            [ventas.pages.admin.users :as users-page]
            [ventas.routes :as routes :refer [go-to]]
            [ventas.util :as util :refer [dispatch-page-event]]
            [ventas.utils.ui :as utils.ui]
            [ventas.pages.admin :as admin]))

(defn user-form []
  (reagent/with-let [data (atom {})]
    [base/form {:on-submit (utils.ui/with-handler #(dispatch-page-event [:submit @data]))}
     [base/form-group {:widths "equal"}
      [base/form-input (utils.ui/wrap-with-model {:label "Nombre"
                                                  :model data
                                                  :name "name"})]
      [base/form-input (utils.ui/wrap-with-model {:label "Email"
                                                  :model data
                                                  :name "email"
                                                  :type "email"})]
      [base/form-input (utils.ui/wrap-with-model {:label "Contraseña"
                                                  :model data
                                                  :name "password"
                                                  :type "password"})]]
     [base/form-group {:widths "equal"}
      [base/form-textarea (utils.ui/wrap-with-model {:label "Sobre mí"
                                                     :model data
                                                     :name "description"})]]
     [base/form-group {:widths "equal"}
      [base/form-field
       [:label "Roles"]
       [base/dropdown (utils.ui/wrap-with-model {:model data
                                                 :name "roles"
                                                 :multiple true
                                                 :fluid true
                                                 :selection true
                                                 :options @(rf/subscribe [:app.reference/user.role])})]]]
     [base/form-button {:type "submit"} "Enviar"]]))

(defmethod pages :admin.users.edit []
  [admin/skeleton
   [:div.admin-users-edit__page
    [user-form]]])
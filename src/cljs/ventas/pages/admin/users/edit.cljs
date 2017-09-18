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
            [ventas.routes :as routes]
            [ventas.util :as util :refer [dispatch-page-event]]
            [ventas.utils.ui :as utils.ui]
            [ventas.pages.admin :as admin]))

(defn user-form []
  (let [user-kw ::user
        data (atom {})
        key (atom {})
        user-id (get-in (routes/current) [:route-params :id])]
    (rf/dispatch [:api/entities.find
                  user-id
                  {:success-fn (fn [user]
                                 (reset! data user)
                                 (reset! key (hash user)))}])
    (rf/dispatch [:ventas/reference.user.role])
    (fn []
      (debug "data" @data)
      ^{:key @key}
      [base/form {:on-submit (utils.ui/with-handler #(dispatch-page-event [:submit @data]))}
       [base/form-group {:widths "equal"}
        [base/form-input
         {:label "Nombre"
          :default-value (:name @data)
          :on-change #(swap! data assoc :name (-> % .-target .-value))}]
        [base/form-input
         {:label "Email"
          :default-value (:email @data)
          :on-change #(swap! data assoc :email (-> % .-target .-value))}]]
       [base/form-group {:widths "equal"}
        [base/form-textarea
         {:label "Sobre mí"
          :default-value (:description @data)
          :on-change #(swap! data assoc :description (-> % .-target .-value))}]]
       [base/form-group {:widths "equal"}
        [base/form-field
         [:label "Roles"]
         [base/dropdown
          {:multiple true
           :fluid true
           :selection true
           :options @(rf/subscribe [:reference.user.role])}]]]
       [base/form-button {:type "submit"} "Enviar"]])))

(defmethod pages :admin.users.edit []
  [admin/skeleton
   [:div.admin-users-edit__page
    [user-form]]])
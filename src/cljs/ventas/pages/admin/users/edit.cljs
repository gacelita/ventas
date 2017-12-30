(ns ventas.pages.admin.users.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(defn- reference-options [ref]
  (map #(update % :value str)
       @(rf/subscribe [::events/db [:enums ref]])))

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [::backend/users.save {:params data
                                     :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::user-saved-notification)
                                  :theme "success"}]
    :go-to [:admin.users]}))

(def state-key ::state)

(rf/reg-event-db
  ::set-field
  (fn [db [_ k v]]
    (assoc-in db [state-key :user k] v)))

(defn user-form []
  (rf/dispatch [::backend/entities.find
                (get-in (routes/current) [:route-params :id])
                {:success [::events/db [state-key :user]]}])
  (rf/dispatch [::events/enums.get :user.role])
  (fn []
    (let [{:keys [user]} @(rf/subscribe [::events/db state-key])
          {:keys [first-name email description roles id last-name phone company status culture]} user]
      (js/console.log "user" user)

      [base/form {:key id
                  :on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}
       [base/form-input
        {:label (i18n ::first-name)
         :default-value first-name
         :on-change #(rf/dispatch [::set-field :first-name (-> % .-target .-value)])}]

       [base/form-input
        {:label (i18n ::last-name)
         :default-value last-name
         :on-change #(rf/dispatch [::set-field :last-name (-> % .-target .-value)])}]

       [base/form-input
        {:label (i18n ::email)
         :default-value email
         :on-change #(rf/dispatch [::set-field :email (-> % .-target .-value)])}]

       [base/form-input
        {:label (i18n ::phone)
         :default-value phone
         :on-change #(rf/dispatch [::set-field :phone (-> % .-target .-value)])}]

       [base/form-input
        {:label (i18n ::company)
         :default-value company
         :on-change #(rf/dispatch [::set-field :company (-> % .-target .-value)])}]

       [base/form-field
        [:label (i18n ::culture)]
        [base/dropdown
         {:options (reference-options :i18n.culture)
          :default-value culture
          :selection true
          :on-change #(rf/dispatch [::set-field :culture (.-value %2)])}]]

       [base/form-field
        [:label (i18n ::roles)]
        [base/dropdown
         {:multiple true
          :fluid true
          :selection true
          :options (reference-options :user.role)
          :default-value (map str roles)
          :on-change #(rf/dispatch [::set-field :roles (set (.-value %2))])}]]

       [base/form-field
        [:label (i18n ::status)]
        [base/dropdown
         {:options (reference-options :user.status)
          :selection true
          :default-value status
          :on-change #(rf/dispatch [::set-field :status (.-value %2)])}]]

       [base/form-button {:type "submit"} (i18n ::submit)]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users-edit__page
    [user-form]]])

(routes/define-route!
 :admin.users.edit
 {:name ::page
  :url [:id "/edit"]
  :component page})
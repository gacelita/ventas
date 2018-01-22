(ns ventas.pages.admin.users.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
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

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} [_ data]]
   {:dispatch [::backend/users.save {:params (get-in db [state-key :user])
                                     :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::user-saved-notification)
                                  :theme "success"}]
    :go-to [:admin.users]}))

(rf/reg-event-db
  ::set-field
  (fn [db [_ k v]]
    (assoc-in db [state-key :user k] v)))

(defn user-form []
  (rf/dispatch [::backend/entities.find (routes/ref-from-param :id)
                {:success [::events/db [state-key :user]]}])
  (rf/dispatch [::events/enums.get :user.role])
  (rf/dispatch [::events/enums.get :user.status])
  (rf/dispatch [::events/i18n.cultures.list])
  (fn []
    (let [{:keys [user]} @(rf/subscribe [::events/db state-key])
          {:keys [first-name email description roles id last-name phone company status culture]} user]

      [base/form {:key id
                  :on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

       [base/segment {:color "orange"
                      :title "User"}
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

        [base/form-field
         [:label (i18n ::status)]
         [base/dropdown
          {:options @(rf/subscribe [::events/db [:enums :user.status]])
           :selection true
           :default-value status
           :on-change #(rf/dispatch [::set-field :status (.-value %2)])}]]]

       [base/divider {:hidden true}]

       [base/segment {:color "orange"
                      :title "Contact information"}
        [base/form-input
         {:label (i18n ::phone)
          :default-value phone
          :on-change #(rf/dispatch [::set-field :phone (-> % .-target .-value)])}]

        [base/form-input
         {:label (i18n ::company)
          :default-value company
          :on-change #(rf/dispatch [::set-field :company (-> % .-target .-value)])}]]

       [base/divider {:hidden true}]

       [base/segment {:color "orange"
                      :title "Configuration"}
        [base/form-field
         [:label (i18n ::culture)]
         [base/dropdown
          {:options @(rf/subscribe [::events/db :cultures])
           :default-value culture
           :selection true
           :on-change #(rf/dispatch [::set-field :culture (.-value %2)])}]]

        [base/form-field
         [:label (i18n ::roles)]
         [base/dropdown
          {:multiple true
           :selection true
           :options @(rf/subscribe [::events/db [:enums :user.role]])
           :default-value roles
           :on-change #(rf/dispatch [::set-field :roles (set (.-value %2))])}]]]

       [base/divider {:hidden true}]

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
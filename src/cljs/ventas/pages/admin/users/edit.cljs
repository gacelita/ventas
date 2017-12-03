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

(defn- role-options []
  (map #(update % :value str) @(rf/subscribe [::events/db [:reference :user.role]])))

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [::backend/users.save {:params data
                                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::user-saved-notification) :theme "success"}]
    :go-to [:admin.users]}))

(defn user-form []
  (let [data (atom {})
        key (atom nil)]
    (rf/dispatch [::backend/entities.find
                  (get-in (routes/current) [:route-params :id])
                  {:success (fn [user]
                                 (reset! data user)
                                 (reset! key (hash user)))}])
    (rf/dispatch [::events/reference :user.role])
    (fn []
      ^{:key @key}
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit @data]))}
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
           :options (role-options)
           :default-value (map str (:roles @data))
           :on-change #(swap! data assoc :roles (set (.-value %2)))}]]]
       [base/form-button {:type "submit"} "Enviar"]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-users-edit__page
    [user-form]]])

(routes/define-route!
 :admin.users.edit
 {:name (i18n ::page)
  :url [:id "/edit"]
  :component page})
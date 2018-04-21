(ns ventas.themes.clothing.pages.frontend.profile.account
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.session :as session]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.themes.clothing.pages.frontend.profile.skeleton :as profile.skeleton]
   [ventas.utils.ui :as utils.ui]
   [ventas.utils.validation :as validation])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(def regular-length-validator [::length-error validation/length-validator {:max 30}])

(def form-config
  {:db-path [state-key]
   :validators {::first-name [regular-length-validator]
                ::last-name [regular-length-validator]
                ::company [regular-length-validator]
                ::email [[::email-error validation/email-validator]]
                ::phone [regular-length-validator]
                ::privacy-policy [[::required-error validation/required-validator]]}})

(defn- field [{:keys [key inline-label] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (when-not inline-label
                               (i18n (ns-kw (if (sequential? key)
                                              (first key)
                                              key))))})])

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/users.save
               {:params (form/get-data db [state-key])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch-n [[::notificator/notify-saved]
                 [::cancel-edition]]}))

(rf/reg-event-fx
 ::submit-password
 (fn [{:keys [db]}]
   {:dispatch [::backend/users.change-password
               {:params {:password (:password (form/get-data db [state-key]))}
                :success ::submit-password.next}]}))

(rf/reg-event-fx
 ::submit-password.next
 (fn [_]
   {:dispatch-n [[::form/set-field [state-key] :password nil :reset-hash? true]
                 [::form/set-field [state-key] :password-repeat nil :reset-hash? true]
                 [::notificator/notify-saved nil]]}))

(defn content []
  [form/form [state-key]
   [:div
    [base/segment
     [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

      [base/form-group
       [field {:key :first-name
               :width 5}]
       [field {:key :last-name
               :width 11}]]

      [base/form-group
       [field {:key :company
               :width 16}]]

      [base/form-group
       [field {:key :email
               :width 8}]
       [field {:key :phone
               :width 8}]]

      [base/form-group
       [field {:key :privacy-policy
               :type :checkbox
               :inline-label (reagent/as-element
                              [:label (str (i18n ::privacy-policy-text) " ")
                               [:a {:href (routes/path-for :frontend.privacy-policy)}
                                (i18n ::privacy-policy)]])}]]

      [base/form-button {:type "submit"}
       (i18n ::submit)]]]

    [base/segment
     [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit-password]))}
      [field {:key :password
              :type :password}]
      [field {:key :password-repeat
              :type :password}]
      [base/form-button {:type "submit"}
       (i18n ::submit)]]]]])

(defn page []
  [profile.skeleton/skeleton
   [content]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [identity (session/get-identity)]
     {:dispatch-n [[::session/require-identity]
                   [::form/populate form-config identity]]})))

(routes/define-route!
  :frontend.profile.account
  {:name ::page
   :url ["account"]
   :component page
   :init-fx [::init]})

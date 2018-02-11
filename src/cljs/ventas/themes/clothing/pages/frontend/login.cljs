(ns ventas.themes.clothing.pages.frontend.login
  (:require
   [re-frame.core :as rf]
   [reagent.core :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as utils]
   [ventas.utils.logging :refer [debug error info trace warn]]))

(def state-key ::state)

(defn- login []
  (let [{:keys [form]} @(rf/subscribe [::events/db [state-key :login]])]
    [base/segment {:class "login-page__segment"}
     [:h3 (i18n ::login)]
     [base/form
      [base/form-field
       [:input {:placeholder (i18n ::email)
                :on-change (utils/value-handler
                            #(rf/dispatch [::form/set-field [state-key :login] :email %]))}]]
      [base/form-field
       [:input {:placeholder (i18n ::password)
                :type "password"
                :on-change (utils/value-handler
                            #(rf/dispatch [::form/set-field [state-key :login] :password %]))}]]
      [:a.login-page__forgot-password {:href "/"}
       (i18n ::forgot-password)]
      [base/button {:type "button"
                    :on-click #(rf/dispatch [::events/users.login form])}
       (i18n ::login)]]]))

(defn- register []
  (let [{:keys [form]} @(rf/subscribe [::events/db [state-key :register]])]
    [base/segment {:class "login-page__segment"}
     [:h3 (i18n ::register)]
     [base/form
      [base/form-field
       [:input {:placeholder (i18n ::full-name)
                :on-change (utils/value-handler
                            #(rf/dispatch [::form/set-field [state-key :register] :name %]))}]]
      [base/form-field
       [:input {:placeholder (i18n ::email)
                :on-change (utils/value-handler
                            #(rf/dispatch [::form/set-field [state-key :register] :email %]))}]]
      [base/form-field
       [:input {:placeholder (i18n ::password)
                :type "password"
                :on-change (utils/value-handler
                            #(rf/dispatch [::form/set-field [state-key :register] :password %]))}]]
      [base/button {:type "button"
                    :on-click #(rf/dispatch [::events/users.register form])}
       (i18n ::register)]]]))

(defn page []
  [skeleton
   [:div.login-page
    [base/container
     [login]
     [register]]]])

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   (let [{:keys [id status]} (get-in db [:session :identity])]
     (when (and id (not= status :user.status/unregistered))
       {:go-to [:frontend.profile]}))))

(routes/define-route!
  :frontend.login
  {:name ::page
   :url ["login"]
   :component page
   :init-fx [::init]})

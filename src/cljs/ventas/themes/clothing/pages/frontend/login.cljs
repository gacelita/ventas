(ns ventas.themes.clothing.pages.frontend.login
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.page :refer [pages]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.i18n :refer [i18n]]
   [ventas.components.base :as base]
   [ventas.utils :as utils]
   [ventas.components.notificator :as notificator]
   [ventas.components.sidebar :as sidebar]
   [ventas.routes :as routes]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(defn- login []
  (reagent/with-let [data (atom {})]
    [base/segment {:class "login-page__segment"}
     [:h3 (i18n ::login)]
     [base/form
      [base/form-field
       [:input {:placeholder (i18n ::email)
                :on-change (utils/value-handler #(swap! data assoc :email %))}]]
      [base/form-field
       [:input {:placeholder (i18n ::password)
                :type "password"
                :on-change (utils/value-handler #(swap! data assoc :password %))}]]
      [:a.login-page__forgot-password {:href "/"}
       (i18n ::forgot-password)]
      [base/button {:type "button"
                    :on-click #(rf/dispatch [::events/users.login @data])}
       (i18n ::login)]]]))

(defn- register []
  (reagent/with-let [data (atom {})]
    [base/segment {:class "login-page__segment"}
     [:h3 (i18n ::register)]
     [base/form
      [base/form-field
       [:input {:placeholder (i18n ::full-name)
                :on-change (utils/value-handler #(swap! data assoc :name %))}]]
      [base/form-field
       [:input {:placeholder (i18n ::email)
                :on-change (utils/value-handler #(swap! data assoc :email %))}]]
      [base/form-field
       [:input {:placeholder (i18n ::password)
                :type "password"
                :on-change (utils/value-handler #(swap! data assoc :password %))}]]
      [base/button {:type "button"
                    :on-click #(rf/dispatch [::events/users.register @data])}
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
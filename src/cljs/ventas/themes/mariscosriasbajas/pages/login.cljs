(ns ventas.themes.mariscosriasbajas.pages.login
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.page :refer [pages]]
   [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]
   [ventas.i18n :refer [i18n]]
   [ventas.components.base :as base]
   [ventas.utils :as util :refer [value-handler]]
   [ventas.components.notificator]))

(defn- login-successful [{:keys [user token]}]
  (rf/dispatch [::notificator/add {:message (i18n ::session-started)}])
  (rf/dispatch [::login-success user token]))

(rf/reg-event-fx
  ::login-success
  [(rf/inject-cofx :local-storage)]
  (fn [{:keys [db local-storage]} [_ user token]]
    {:db (assoc db :session user)
     :local-storage (assoc local-storage :token token)}))

(rf/reg-event-fx
  ::login
  (fn [cofx [_ {:keys [email password]}]]
    {:dispatch [:api/users.login
                {:params {:email email
                          :password password}
                 :success-fn login-successful}]}))

(rf/reg-event-fx
 ::register
 (fn [cofx [_ {:keys [name email password]}]]
   {:dispatch [:api/users.register
               {:params {:name name
                         :email email
                         :password password}
                :success-fn #(rf/dispatch [::notificator/add {:message (i18n ::user-registered)}])}]}))

(defn- login []
  (reagent/with-let [data (atom {})]
    [base/segment {:class "login-page__segment"}
     [:h3 (i18n ::login)]
     [base/form
      [base/form-field
       [:input {:placeholder (i18n ::email)
                :on-change (value-handler #(swap! data assoc :email %))}]]
      [base/form-field
       [:input {:placeholder (i18n ::password)
                :type "password"
                :on-change (value-handler #(swap! data assoc :password %))}]]
      [:a.login-page__forgot-password {:href "/"}
       (i18n ::forgot-password)]
      [base/button {:type "button"
                    :on-click #(rf/dispatch [::login @data])}
       (i18n ::login)]]]))

(defn- register []
  (reagent/with-let [data (atom {})]
    [base/segment {:class "login-page__segment"}
     [:h3 (i18n ::register)]
     [base/form
      [base/form-field
       [:input {:placeholder (i18n ::full-name)
                :on-change (value-handler #(swap! data assoc :name %))}]]
      [base/form-field
       [:input {:placeholder (i18n ::email)
                :on-change (value-handler #(swap! data assoc :email %))}]]
      [base/form-field
       [:input {:placeholder (i18n ::password)
                :type "password"
                :on-change (value-handler #(swap! data assoc :password %))}]]
      [base/button {:type "button"
                    :on-click #(rf/dispatch [::register @data])}
       (i18n ::register)]]]))

(defmethod pages :frontend.login []
  [skeleton
   [:div.login-page
    (let [session @(rf/subscribe [:session])]
      (if (seq session)
        [base/container
         [:p (i18n ::welcome (:name session))]
         [base/button
          [base/icon {:name "user"}]
          (i18n ::my-profile)]
         [base/button
          [base/icon {:name "unordered list"}]
          (i18n ::my-orders)]
         [base/button
          [base/icon {:name "address book"}]
          (i18n ::my-addresses)]]

        [base/container
         [login]
         [register]]))]])
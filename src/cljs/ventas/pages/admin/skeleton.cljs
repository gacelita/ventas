(ns ventas.pages.admin.skeleton
  (:require
   [ventas.routes :as routes]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.utils.ui :as utils.ui]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]))

(def configuration-items
  [{:route :admin.configuration.image-sizes :label ::image-sizes}])

(def menu-items
  [{:route :admin.users :label ::users}
   {:route :admin.products :label ::products}
   {:route :admin.orders :label ::orders}
   {:route :admin.plugins :label ::plugins}
   {:route :admin.taxes :label ::taxes}
   {:route :admin.activity-log :label ::activity-log}
   {:route :admin.configuration.image-sizes
    :label ::configuration
    :children configuration-items}])

(defn- menu-item [{:keys [route label children]}]
  [:li
   [:a {:href (routes/path-for route)}
    (i18n label)]
   (when children
     [:ul
      (for [child children]
        ^{:key (hash child)} [menu-item child])])])

(defn- menu []
  [:ul
   (for [item menu-items]
     ^{:key (hash item)} [menu-item item])])

(def state-key ::state)

(rf/reg-event-fx
  ::login
  (fn [{:keys [db]} _]
    (let [{:keys [email password]} (get db state-key)]
      {:dispatch [::events/users.login {:email email
                                        :password password}]})))

(rf/reg-event-db
  ::set-field
  (fn [db [_ k v]]
    (assoc-in db [state-key k] v)))

(defn- login []
  [:div.admin__login-wrapper
   [base/segment {:color "orange" :class "admin__login"}
    [:div.admin__login-image
     [:img {:src "/files/logo"}]]

    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::login]))}
     [base/form-input
      {:placeholder (i18n ::email)
       :on-change #(rf/dispatch [::set-field :email (-> % .-target .-value)])}]

     [base/form-input
      {:placeholder (i18n ::password)
       :type :password
       :on-change #(rf/dispatch [::set-field :password (-> % .-target .-value)])}]

     [base/form-button {:type "submit"} (i18n ::login)]]]])

(defn- content-view [content]
  (rf/dispatch [::backend/admin.brands.list
                {:success [::events/db [:admin :brands]]}])
  (rf/dispatch [::backend/admin.taxes.list
                {:success [::events/db [:admin :taxes]]}])
  (rf/dispatch [::backend/admin.currencies.list
                {:success [::events/db [:admin :currencies]]}])
  (rf/dispatch [::events/i18n.cultures.list])
  (fn []
    [:div
     [:div.admin__userbar
      [:div.admin__userbar-logo
       [:img {:src "/files/logo"}]]
      [:div.admin__userbar-home
       [:a {:href (routes/path-for :frontend)}
        [base/icon {:name "home"}]
        [:span (i18n ::home)]]]
      (let [{:keys [identity]} @(rf/subscribe [::events/db [:session]])]
        [:div.admin__userbar-profile
         [base/dropdown {:text (:first-name identity)
                         :class "dropdown--align-right"}
          [base/dropdown-menu
           [base/dropdown-item {:text (i18n ::logout)
                                :on-click #(rf/dispatch [::events/session.stop])}]]]])]
     [:div.admin__skeleton
      [:div.admin__sidebar
       [:a {:href (routes/path-for :admin)}
        [:h3 (i18n ::administration)]]
       [menu]]
      [:div.admin__content
       content]]]))

(defn skeleton [content]
  [:div.root
   [ventas.notificator/notificator]
   [ventas.popup/popup]
   (let [{:keys [identity]} @(rf/subscribe [::events/db [:session]])]
     (if-not (contains? (set (:roles identity)) :user.role/administrator)
       [login]
       [content-view content]))])

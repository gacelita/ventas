(ns ventas.pages.admin.skeleton
  (:require
    [ventas.routes :as routes]
    [ventas.events :as events]
    [ventas.i18n :refer [i18n]]
    [re-frame.core :as rf]
    [ventas.components.base :as base]
    [ventas.utils.ui :as utils.ui]))

(def configuration-items
  [{:route :admin.configuration.image-sizes :label ::image-sizes}])

(def menu-items
  [{:route :admin.users :label ::users}
   {:route :admin.products :label ::products}
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
       :on-change #(rf/dispatch [::set-field :password (-> % .-target .-value)])}]

     [base/form-button {:type "submit"} (i18n ::login)]]]])

(defn skeleton [content]
  (let [session @(rf/subscribe [::events/db [:session]])]
    (if-not (contains? (set (get-in session [:identity :roles])) :user.role/administrator)
      [login]
      [:div.admin__skeleton
       [:div.admin__sidebar
        [:a {:href (routes/path-for :admin)}
         [:h3 (i18n ::administration)]]
        [menu]]
       [:div.admin__content
        content]])))

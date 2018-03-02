(ns ventas.pages.admin.skeleton
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.popup :as popup]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.ws :as ws]))

(def configuration-items
  [{:route :admin.configuration.image-sizes
    :label ::image-sizes}
   {:route :admin.configuration.email
    :label ::email}])

(def menu-items
  [{:route :admin
    :label ::dashboard
    :icon "home"}

   {:route :admin.products
    :label ::products
    :icon "tag"
    :children [{:route :admin.products.discounts
                :label ::discounts}]}

   {:route :admin.orders
    :label ::orders
    :icon "unordered list"}

   {:route :admin.users
    :label ::users
    :icon "user"}

   {:route :admin.plugins
    :label ::plugins
    :icon "plug"}

   {:route :admin.taxes
    :label ::taxes
    :icon "percent"}

   {:route :admin.payment-methods
    :label ::payment-methods
    :icon "payment"}

   {:divider true}

   {:route :admin.activity-log
    :label ::activity-log
    :icon "time"}

   {:route :admin.configuration.image-sizes
    :label ::configuration
    :icon "configure"
    :children configuration-items}])

(defn- menu-item [{:keys [route label icon children] :as item}]
  [:li.admin__menu-item (when (= (routes/handler) route)
                          {:class "admin__menu-item--active"})
   (if (:divider item)
     [base/divider]
     (list
      [:a {:href (routes/path-for route)}
       (when icon
         [base/icon {:name icon}])
       (i18n label)]
      (when children
        [:ul
         (for [child children]
           ^{:key (hash child)} [menu-item child])])))])

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

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/admin.brands.list
                  {:success [::events/db [:admin :brands]]}]
                 [::backend/admin.taxes.list
                  {:success [::events/db [:admin :taxes]]}]
                 [::backend/admin.currencies.list
                  {:success [::events/db [:admin :currencies]]}]
                 [::events/i18n.cultures.list]]}))

(defn- content-view [content]
  (rf/dispatch [::init])
  (fn []
    [:div
     [:div.admin__userbar
      [:div.admin__userbar-logo
       [:img {:src "/files/logo"}]]
      [:div.admin__userbar-home
       [:a {:href (routes/path-for :frontend)}
        [base/icon {:name "home"}]
        [:span (i18n ::home)]]]
      [base/loader {:active (boolean (seq @(rf/subscribe [::ws/pending-requests])))
                    :inverted true
                    :size "small"}]
      (let [{:keys [identity]} @(rf/subscribe [::events/db [:session]])]
        [:div.admin__userbar-profile
         [base/dropdown {:text (:first-name identity)
                         :class "dropdown--align-right"}
          [base/dropdown-menu
           [base/dropdown-item {:text (i18n ::logout)
                                :on-click #(rf/dispatch [::events/users.logout])}]]]])]
     [:div.admin__skeleton
      [:div.admin__sidebar
       [:a {:href (routes/path-for :admin)}
        [:h3 (i18n ::administration)]]
       [menu]]
      [:div.admin__content
       content]]]))

(defn skeleton [content]
  [:div.root
   [notificator/notificator]
   [popup/popup]
   (let [{:keys [identity]} @(rf/subscribe [::events/db [:session]])]
     (if-not (contains? (set (:roles identity)) :user.role/administrator)
       [login]
       [content-view content]))])

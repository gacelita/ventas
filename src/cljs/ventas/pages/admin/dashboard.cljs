(ns ventas.pages.admin.dashboard
  (:require
   [re-frame.core :as rf]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [clojure.string :as str]))

(def state-key ::state)

(defn- segment [{:keys [label]} & children]
  [base/grid-column
   [base/segment
    [base/header {:as "h3"}
     label]
    children]])

(defn- user-view [{:keys [id first-name last-name]}]
  [:li
   [:a {:href (routes/path-for :admin.users.edit :id id)}
    [:p (str/join " " [first-name last-name])]]])

(defn- content []
  (rf/dispatch [::backend/admin.users.list
                {:success [::events/db [state-key :users]]}])
  (fn []
    (let [{:keys [users]} @(rf/subscribe [::events/db [state-key]])]
      [base/grid {:stackable true :columns 2}
       [segment {:label (i18n ::traffic-statistics)}]
       [segment {:label (i18n ::pending-orders)}]
       [segment {:label (i18n ::latest-users)}
        [:ul.admin-dashboard__users
         (for [user users]
           ^{:key (:id user)} [user-view user])]]
       [segment {:label (i18n ::unread-messages)}]])))

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-dashboard__page
    [content]]])

(routes/define-route!
  :admin.dashboard
  {:name ::page
   :url "dashboard"
   :component page})

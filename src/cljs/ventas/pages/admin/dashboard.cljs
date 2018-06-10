(ns ventas.pages.admin.dashboard
  (:require
   [cljsjs.moment]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.chart :as chart]
   [ventas.components.datepicker :as datepicker]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.statistics :as admin.statistics]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(def state-key ::state)

(defn- segment [{:keys [label]} & children]
  [base/grid-column
   [base/segment
    [base/header {:as "h3"}
     label]
    children]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/admin.users.list
                  {:success [::events/db [state-key :users]]}]
                 [::admin.statistics/view.select :24h]]}))

(defn- user-view [{:keys [id first-name last-name created-at]}]
  [:li.admin-dashboard__user
   [:a {:href (routes/path-for :admin.users.edit :id id)}
    [:p (if (or first-name last-name)
          (str/join " " [first-name last-name])
          (i18n ::no-name))]
    [:p (str created-at)]]])

(defn- latest-users []
  (let [{:keys [users]} @(rf/subscribe [::events/db [state-key]])]
    [:ul.admin-dashboard__users
     (doall
      (for [user users]
        ^{:key (:id user)}
        [user-view user]))]))

(defn- content []
  [base/grid {:stackable true :columns 2}
   [base/grid-column
    [:div.admin-dashboard__traffic-statistics
     [segment {:label (i18n ::traffic-statistics)}
      [admin.statistics/view-options [[:24h (i18n ::admin.statistics/twenty-four-hours)]
                                      [:week (i18n ::admin.statistics/week)]
                                      [:month (i18n ::admin.statistics/month)]]]
      [admin.statistics/traffic-stats-chart]]]]
   [segment {:label (i18n ::pending-orders)}]
   [segment {:label (i18n ::latest-users)}
    [latest-users]]
   [segment {:label (i18n ::unread-messages)}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-dashboard__page
    [content]]])

(ns ventas.pages.admin.dashboard
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.chart :as chart]
   [cljsjs.moment]))

(def state-key ::state)

(defn- segment [{:keys [label]} & children]
  [base/grid-column
   [base/segment
    [base/header {:as "h3"}
     label]
    children]])

(defn- user-view [{:keys [id first-name last-name created-at]}]
  [:li.admin-dashboard__user
   [:a {:href (routes/path-for :admin.users.edit :id id)}
    [:p (if (or first-name last-name)
          (str/join " " [first-name last-name])
          (i18n ::no-name))]
    [:p created-at]]])

(def chart (atom nil))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/admin.users.list
                  {:success [::events/db [state-key :users]]}]
                 [::backend/admin.stats.realtime
                  {:params {:topics ["navigation" "http"]
                            :min (- (.getTime (js/Date.)) 3600000)}
                   :success ::stats.update}]]}))

(rf/reg-event-fx
 ::stats.update
 (fn [_ [_ buckets]]
   (let [counts (map :doc_count buckets)
         keys (->> buckets (map :key) (map js/moment))]
     {:dispatch [::chart/update {:id ::chart
                                 :data-fn #(into % counts)
                                 :labels-fn #(into % keys)}]})))

(defn- traffic-stats []
  [chart/chart
   {:height 200
    :id ::chart
    :config {:type "line"
             :data {:labels []
                    :datasets [{:data []
                                :label "Navigation and HTTP events"
                                :backgroundColor "#90EE90"
                                :fill false}]}
             :options {:responsive true
                       :maintainAspectRatio false
                       :scales {:xAxes [{:type "time"
                                         :time {:format "MM/DD/YYYY HH:mm"
                                                :tooltipFormat "ll HH:mm"}}]}}}}])

(defn- latest-users []
  (let [{:keys [users]} @(rf/subscribe [::events/db [state-key]])]
    [:ul.admin-dashboard__users
     (doall
      (for [user users]
        ^{:key (:id user)}
        [user-view user]))]))

(defn- content []
  [base/grid {:stackable true :columns 2}
   [segment {:label (i18n ::traffic-statistics)}
    [traffic-stats]]
   [segment {:label (i18n ::pending-orders)}]
   [segment {:label (i18n ::latest-users)}
    [latest-users]]
   [segment {:label (i18n ::unread-messages)}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-dashboard__page
    [content]]])
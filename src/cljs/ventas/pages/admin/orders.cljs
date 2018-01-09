(ns ventas.pages.admin.orders
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [day8.re-frame.forward-events-fx]
   [re-frame-datatable.core :as dt]
   [ventas.page :refer [pages]]
   [ventas.utils :as utils]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [ventas.components.table :as table]
   [clojure.string :as str]))

(def state-key ::state)

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true :on-click #(routes/go-to :admin.orders.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true :on-click #(rf/dispatch [::events/admin.entities.remove id])}
    [base/icon {:name "remove"}]]])

(defn- status-column [{:keys [status]}]
  [:span (i18n status)])

(defn- amount-column [{:keys [amount]}]
  [:div amount])

(defn- footer []
  [base/button
   {:on-click #(routes/go-to :admin.orders.edit :id 0)}
   (i18n ::create)])

(rf/reg-event-fx
  ::fetch
  (fn [{:keys [db]} [_ {:keys [state-path]}]]
    (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
      {:dispatch [::backend/admin.orders.list
                  {:success ::fetch.next
                   :params {:pagination {:page page
                                         :items-per-page items-per-page}
                            :sorting {:direction sort-direction
                                      :field sort-column}}}]})))

(rf/reg-event-fx
  ::fetch.next
  (fn [{:keys [db]} [_ {:keys [items total]}]]
    (let [users (map :user items)]
      {:dispatch-n (for [user users]
                     [::backend/admin.entities.find-json
                      {:params {:id user}
                       :success [::events/db [state-key :users user]]}])
       :db (-> db
               (assoc-in [state-key :orders] items)
               (assoc-in [state-key :table :total] total))})))

(defn- user-column [{:keys [user id]}]
  (when-let [data @(rf/subscribe [::events/db [state-key :users user]])]
    [:a {:href (routes/path-for :admin.orders.edit :id id)}
     (str/join " " [(:first-name data) (:last-name data)])]))

(defn- content []
  [:div.admin-orders__table
   [table/table
    {:init-state {:page 0
                  :items-per-page 5
                  :sort-column :id
                  :sort-direction :asc}
     :state-path [state-key :table]
     :data-path [state-key :orders]
     :fetch-fx ::fetch
     :columns [{:id :user
                :label (i18n ::user)
                :component user-column}
               {:id :amount
                :label (i18n ::amount)
                :component amount-column}
               {:id :status
                :label (i18n ::status)
                :component status-column}
               {:id :actions
                :label (i18n ::actions)
                :component action-column}]
     :footer footer}]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-orders__page
    [content]]])

(routes/define-route!
 :admin.orders
 {:name ::page
  :url "orders"
  :component page})
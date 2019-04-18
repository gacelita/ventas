(ns ventas.themes.admin.orders
  (:require
   [clojure.string :as str]
   [day8.re-frame.forward-events-fx]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.events :as events]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(def state-key ::state)

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true :on-click #(rf/dispatch [::events/admin.entities.remove [state-key :table :rows] id])}
    [base/icon {:name "remove"}]]])

(defn- status-column [{:keys [status]}]
  [:span (:name status)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ state-path]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (table/get-state db state-path)]
     {:dispatch [::api.admin/admin.entities.list
                 {:success ::fetch.next
                  :params {:type :order
                           :pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-fx
 ::fetch.next
 (fn [{:keys [db]} [_ {:keys [items total]}]]
   (let [users (map :user items)]
     {:dispatch-n (for [user users]
                    [::api.admin/admin.entities.find-serialize
                     {:params {:id user}
                      :success [:db [state-key :users user]]}])
      :db (-> db
              (assoc-in [state-key :table :rows] items)
              (assoc-in [state-key :table :total] total))})))

(defn- user-column [{:keys [user id]}]
  (when-let [data @(rf/subscribe [:db [state-key :users user]])]
    [:a {:href (routes/path-for :admin.orders.edit :id id)}
     (str/join " " [(:first-name data) (:last-name data)])]))

(defn- content []
  [:div.admin-orders__table
   [table/table [state-key :table]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-orders__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::table/init [state-key :table]
               {:fetch-fx [::fetch]
                :columns [{:id :user
                           :label (i18n ::user)
                           :component user-column}
                          {:id :amount
                           :label (i18n ::amount)
                           :component (partial table/amount-column :amount)}
                          {:id :status
                           :label (i18n ::status)
                           :component status-column}
                          {:id :actions
                           :label (i18n ::actions)
                           :component action-column}]}]}))

(routes/define-route!
  :admin.orders
  {:name ::page
   :url "orders"
   :component page
   :init-fx [::init]})

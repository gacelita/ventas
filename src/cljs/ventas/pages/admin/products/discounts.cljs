(ns ventas.pages.admin.products.discounts
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.components.table :as table]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [re-frame.core :as rf]
   [ventas.utils.formatting :as utils.formatting]))

(def state-key ::state)

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true :on-click #(routes/go-to :admin.products.discounts.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true :on-click #(rf/dispatch [::events/admin.entities.remove [state-key :discounts] id])}
    [base/icon {:name "remove"}]]])

(defn- footer []
  [base/button
   {:on-click #(routes/go-to :admin.products.discounts.edit :id 0)}
   (i18n ::create)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ {:keys [state-path]}]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (get-in db state-path)]
     {:dispatch [::backend/admin.discounts.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-fx
 ::fetch.next
 (fn [{:keys [db]} [_ {:keys [items total]}]]
   {:db (-> db
            (assoc-in [state-key :discounts] items)
            (assoc-in [state-key :table :total] total))}))

(defn- name-column [{:keys [name id]}]
  [:a {:href (routes/path-for :admin.products.discounts.edit :id id)}
   name])

(defn- content []
  [:div.admin-discounts__table
   [table/table
    {:init-state {:page 0
                  :items-per-page 5
                  :sort-column :id
                  :sort-direction :asc}
     :state-path [state-key :table]
     :data-path [state-key :discounts]
     :fetch-fx ::fetch
     :columns [{:id :name
                :label (i18n ::name)
                :component name-column}
               {:id :code
                :label (i18n ::code)}
               {:id :amount
                :label (i18n ::amount)
                :component (partial table/amount-column :amount)}
               {:id :actions
                :label (i18n ::actions)
                :component action-column}]
     :footer footer}]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-discounts__page
    [content]]])

(routes/define-route!
 :admin.products.discounts
 {:name ::page
  :url "discounts"
  :component page})
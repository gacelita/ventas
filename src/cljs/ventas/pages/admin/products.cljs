(ns ventas.pages.admin.products
  (:require
   [day8.re-frame.forward-events-fx]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(def state-key ::state)

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(rf/dispatch [::events/admin.entities.remove [state-key :table :rows] id])}
    [base/icon {:name "remove"}]]])

(defn- footer []
  [base/button
   {:on-click #(routes/go-to :admin.products.edit :id 0)}
   (i18n ::create-product)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ state-path]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (table/get-state db state-path)]
     {:dispatch [::backend/products.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field (if (= sort-column :price)
                                              [:price :value]
                                              sort-column)}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items total]}]]
   (-> db
       (assoc-in [state-key :table :rows] items)
       (assoc-in [state-key :table :total] total))))

(defn- content []
  [:div.admin-products__table
   [table/table [state-key :table]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-products__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::table/init [state-key :table]
               {:fetch-fx [::fetch]
                :footer footer
                :columns [{:id :name
                           :label (i18n ::name)
                           :component (partial table/link-column :admin.products.edit :id :name)}
                          {:id :price
                           :label (i18n ::price)
                           :component (partial table/amount-column :price)}
                          {:id :actions
                           :label (i18n ::actions)
                           :component action-column}]}]}))

(routes/define-route!
  :admin.products
  {:name ::page
   :url "products"
   :component page
   :init-fx [::init]})

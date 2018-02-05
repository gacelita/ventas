(ns ventas.pages.admin.products
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [day8.re-frame.forward-events-fx]
   [ventas.page :refer [pages]]
   [ventas.utils :as utils]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [ventas.components.table :as table]))

(def state-key ::state-cat)

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true :on-click #(routes/go-to :admin.products.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true :on-click #(rf/dispatch [::events/admin.entities.remove id])}
    [base/icon {:name "remove"}]]])

(defn- footer []
  [base/button
   {:on-click #(routes/go-to :admin.products.edit :id 0)}
   (i18n ::create-product)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ {:keys [state-path]}]]
   (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
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
   (let [items (->> items
                    (map #(update % :price :value)))]
     (-> db
         (assoc-in [state-key :products] items)
         (assoc-in [state-key :table :total] total)))))

(defn- content []
  [:div.admin-products__table
   [table/table
    {:init-state {:page 0
                  :items-per-page 5
                  :sort-column :id
                  :sort-direction :asc}
     :state-path [state-key :table]
     :data-path [state-key :products]
     :fetch-fx ::fetch
     :columns [{:id :name
                :label (i18n ::name)}
               {:id :price
                :label (i18n ::price)}
               {:id :actions
                :label (i18n ::actions)
                :component action-column}]
     :footer footer}]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-products__page
    [content]]])

(routes/define-route!
  :admin.products
  {:name ::page
   :url "products"
   :component page})

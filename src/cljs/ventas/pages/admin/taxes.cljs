(ns ventas.pages.admin.taxes
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as dt]
   [ventas.page :refer [pages]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.pages.admin.taxes.edit]
   [ventas.utils.formatting :as formatting]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.components.table :as table]
   [ventas.events :as events]))

(def state-key ::state)

(rf/reg-event-fx
  ::remove
  (fn [cofx [_ id]]
    {:dispatch [::backend/admin.entities.remove
                {:params {:id id}
                 :success [::remove.next id]}]}))

(rf/reg-event-db
  ::remove.next
  (fn [db [_ id]]
    (update-in db
               [state-key :taxes]
               (fn [items]
                 (remove #(= (:id %) id)
                         items)))))

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(routes/go-to :admin.taxes.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true
                 :on-click #(rf/dispatch [::remove id])}
    [base/icon {:name "remove"}]]])

(defn- footer []
  [base/button {:on-click #(routes/go-to :admin.taxes.edit :id 0)}
   (i18n ::create)])

(rf/reg-event-fx
  ::fetch
  (fn [{:keys [db]} [_ {:keys [state-path]}]]
    (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
      {:dispatch [::backend/admin.taxes.list
                  {:success ::fetch.next
                   :params {:pagination {:page page
                                         :items-per-page items-per-page}
                            :sorting {:direction sort-direction
                                      :field sort-column}}}]})))

(rf/reg-event-db
  ::fetch.next
  (fn [db [_ {:keys [items total]}]]
    (-> db
        (assoc-in [state-key :taxes] items)
        (assoc-in [state-key :table :total] total))))

(defn- name-column [{:keys [name id]}]
  [:a {:href (routes/path-for :admin.taxes.edit :id id)}
   name])

(defn- content []
  [:div.admin-taxes__table
   [table/table
    {:init-state {:sort-column :id}
     :state-path [state-key :table]
     :data-path [state-key :taxes]
     :fetch-fx ::fetch
     :columns [{:id :name
                :label (i18n ::name)
                :component name-column}
               {:id :amount
                :label (i18n ::amount)}
               {:id :actions
                :label (i18n ::actions)
                :component action-column
                :width 110}]
     :footer footer}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-taxes__page
    [content action-column]]])

(routes/define-route!
 :admin.taxes
 {:name ::page
  :url "taxes"
  :component page})
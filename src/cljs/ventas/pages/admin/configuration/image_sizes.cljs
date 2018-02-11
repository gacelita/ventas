(ns ventas.pages.admin.configuration.image-sizes
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.configuration.image-sizes.edit]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

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
              [state-key :image-sizes]
              (fn [items]
                (remove #(= (:id %) id)
                        items)))))

(defn- action-column [{:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(routes/go-to :admin.configuration.image-sizes.edit :id id)}
    [base/icon {:name "edit"}]]
   [base/button {:icon true
                 :on-click #(rf/dispatch [::remove id])}
    [base/icon {:name "remove"}]]])

(defn- algorithm-column [{:keys [algorithm]}]
  [:span
   (i18n algorithm)])

(defn- footer []
  [base/button {:on-click #(routes/go-to :admin.configuration.image-sizes.edit :id 0)}
   (i18n ::create)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ {:keys [state-path]}]]
   (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
     {:dispatch [::backend/admin.image-sizes.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items total]}]]
   (-> db
       (assoc-in [state-key :image-sizes] items)
       (assoc-in [state-key :table :total] total))))

(defn- keyword-column [{:keys [keyword id]}]
  [:a {:href (routes/path-for :admin.configuration.image-sizes.edit :id id)}
   keyword])

(defn- content []
  [:div.admin-image-sizes__table
   [table/table
    {:init-state {:sort-column :id}
     :state-path [state-key :table]
     :data-path [state-key :image-sizes]
     :fetch-fx ::fetch
     :columns [{:id :keyword
                :label (i18n ::keyword)
                :component keyword-column}
               {:id :width
                :label (i18n ::width)}
               {:id :height
                :label (i18n ::height)}
               {:id :algorithm
                :label (i18n ::algorithm)
                :component algorithm-column}
               {:id :actions
                :label (i18n ::actions)
                :component action-column
                :width 110}]
     :footer footer}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-image-sizes__page
    [content action-column]]])

(routes/define-route!
  :admin.configuration.image-sizes
  {:name ::page
   :url "image-sizes"
   :component page})

(ns ventas.pages.admin.activity-log
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(def state-key ::state)

(defn- action-column [_ row]
  [:div
   [base/button {:icon true}
    [base/icon {:name "edit"}]]])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ {:keys [state-path]}]]
   (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
     {:dispatch [::backend/admin.events.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items total]}]]
   (-> db
       (assoc-in [state-key :events] items)
       (assoc-in [state-key :table :total] total))))

(defn- type-column [{:keys [type]}]
  [:span (i18n type)])

(defn- entity-type-column [{:keys [entity-type]}]
  [:span (str/capitalize (name entity-type))])

(defn- content []
  [:div.admin-events__table
   [table/table
    {:init-state {:sort-column :id}
     :state-path [state-key :table]
     :data-path [state-key :events]
     :fetch-fx ::fetch
     :columns [{:id :entity-id
                :label (i18n ::entity-id)}
               {:id :entity-type
                :label (i18n ::entity-type)
                :component entity-type-column}
               {:id :type
                :label (i18n ::type)
                :component type-column}]}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-events__page
    [content]]])

(routes/define-route!
  :admin.activity-log
  {:name ::page
   :url "activity-log"
   :component page})

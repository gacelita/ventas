(ns ventas.pages.admin.plugins
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.components.base :as base]
   [ventas.components.table :as table]
   [ventas.events.backend :as backend]
   [re-frame.core :as rf]))

(def state-key ::state)

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ {:keys [state-path]}]]
   (let [{:keys [page items-per-page sort-direction sort-column] :as state} (get-in db state-path)]
     {:dispatch [::backend/admin.plugins.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items total]}]]
   (-> db
       (assoc-in [state-key :plugins] items)
       (assoc-in [state-key :table :total] total))))

(defn- content []
  [:div.admin-plugins__table
   [table/table
    {:init-state {:sort-column :id}
     :state-path [state-key :table]
     :data-path [state-key :plugins]
     :fetch-fx ::fetch
     :columns [{:id :name
                :label (i18n ::name)}
               {:id :version
                :label (i18n ::version)}]}]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-plugins__page
    [content]]])

(routes/define-route!
  :admin.plugins
  {:name ::page
   :url "plugins"
   :component page})

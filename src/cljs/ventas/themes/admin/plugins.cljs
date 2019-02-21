(ns ventas.themes.admin.plugins
  (:require
   [re-frame.core :as rf]
   [ventas.components.table :as table]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]))

(def state-key ::state)

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ state-path]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (table/get-state db state-path)]
     {:dispatch [::api.admin/admin.plugins.list
                 {:success ::fetch.next
                  :params {:pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items total]}]]
   (-> db
       (assoc-in [state-key :table :rows] items)
       (assoc-in [state-key :table :total] total))))

(defn- content []
  [:div.admin-plugins__table
   [table/table [state-key :table]]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-plugins__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::table/init [state-key :table]
               {:fetch-fx [::fetch]
                :columns [{:id :name
                           :label (i18n ::name)}]}]}))

(routes/define-route!
  :admin.plugins
  {:name ::page
   :url "plugins"
   :component page
   :init-fx [::init]})

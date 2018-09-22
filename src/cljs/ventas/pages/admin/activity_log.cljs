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

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ state-path]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (table/get-state db state-path)]
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
       (assoc-in [state-key :table :rows] items)
       (assoc-in [state-key :table :total] total))))

(defn- type-column [{:keys [type]}]
  [:span (i18n type)])

(defn- entity-type-column [{:keys [entity-type]}]
  [:span (str/capitalize (name entity-type))])

(defn- content []
  [:div.admin-events__table
   [table/table [state-key :table]]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-events__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::table/init [state-key :table]
               {:fetch-fx [::fetch]
                :columns [{:id :entity-id
                           :label (i18n ::entity-id)}
                          {:id :entity-type
                           :label (i18n ::entity-type)
                           :component entity-type-column}
                          {:id :type
                           :label (i18n ::type)
                           :component type-column}]}]}))

(routes/define-route!
  :admin.activity-log
  {:name ::page
   :url "activity-log"
   :component page
   :init-fx [::init]})

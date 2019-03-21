(ns ventas.components.crud-table
  "Implementation of ventas.components.table for the most common and boring
   use of it."
  (:require
   [re-frame.core :as rf]
   [ventas.i18n :refer [i18n]]
   [ventas.components.base :as base]
   [ventas.server.api.admin :as api.admin]
   [ventas.routes :as routes]
   [ventas.components.table :as table]
   [ventas.i18n :as i18n]))

(def state-key ::state)

(rf/reg-event-fx
 ::remove
 (fn [_ [_ state-path id]]
   {:pre [state-path id]}
   {:dispatch [::api.admin/admin.entities.remove
               {:params {:id id}
                :success [::remove.next state-path id]}]}))

(rf/reg-event-db
 ::remove.next
 (fn [db [_ state-path id]]
   (update-in db
              (conj state-path :rows)
              (fn [items]
                (remove #(= (:id %) id)
                        items)))))

(defn action-column-component [state-path {:keys [id]}]
  [:div
   [base/button {:icon true
                 :on-click #(rf/dispatch [::remove state-path id])}
    [base/icon {:name "remove"}]]])

(defn action-column [state-path]
  {:id :actions
   :label (i18n ::actions)
   :component (partial #'action-column-component state-path)
   :width 110})

(defn- footer [edit-route]
  [base/button {:on-click #(routes/go-to edit-route :id 0)}
   (i18n ::create)])

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ entity-type state-path]]
   (let [{:keys [page items-per-page sort-direction sort-column]} (table/get-state db state-path)]
     {:dispatch [::api.admin/admin.entities.list
                 {:success [::fetch.next state-path]
                  :params {:type entity-type
                           :pagination {:page page
                                        :items-per-page items-per-page}
                           :sorting {:direction sort-direction
                                     :field sort-column}}}]})))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ state-path {:keys [items total]}]]
   (-> db
       (assoc-in (conj state-path :rows) items)
       (assoc-in (conj state-path :total) total))))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} [_ state-path {:keys [columns edit-route entity-type]} extra-config]]
   {:db (assoc-in db state-path nil)
    :dispatch [::table/init state-path
               (merge {:fetch-fx [::fetch entity-type]
                       :columns columns
                       :footer (partial footer edit-route)}
                      extra-config)]}))

(i18n/register-translations!
 {:en_US {::actions "Actions"
          ::create "Create"
          ::submit "Submit"}
  :es_ES {::actions "Acciones"
          ::create "Nuevo"
          ::submit "Enviar"}})
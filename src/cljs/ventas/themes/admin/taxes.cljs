(ns ventas.themes.admin.taxes
  (:require
   [re-frame.core :as rf]
   [ventas.components.table :as table]
   [ventas.components.crud-table :as crud-table]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.themes.admin.taxes.edit]
   [ventas.routes :as routes]))

(def state-path [::state :table])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-taxes__page
    [:div.admin-taxes__table
     [table/table state-path]]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::crud-table/init state-path
               {:columns [{:id :name
                           :label (i18n ::name)
                           :component (partial table/link-column :admin.taxes.edit :id :name)}
                          {:id :amount
                           :label (i18n ::amount)
                           :component (partial table/amount-column :amount)}
                          (crud-table/action-column state-path)]
                :edit-route :admin.taxes.edit
                :entity-type :tax}]}))

(routes/define-route!
  :admin.taxes
  {:name ::page
   :url "taxes"
   :component page
   :init-fx [::init]})

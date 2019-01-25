(ns ventas.themes.admin.customization.menus
  (:require
   [ventas.routes :as routes]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.components.table :as table]
   [ventas.components.crud-table :as crud-table]
   [ventas.themes.admin.customization.menus.edit]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]))

(def state-path [::state :table])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-menus__page
    [:div.admin-menus__table
     [table/table state-path]]]])

(defn- items-column [data]
  [:p (->> (:items data)
           (map :name)
           (interpose ", ")
           (apply str))])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::crud-table/init state-path
               {:columns [{:id :name
                           :label (i18n ::name)
                           :component (partial table/link-column :admin.customization.menus.edit :id :name)}
                          {:id :items
                           :label (i18n ::items)
                           :component items-column}
                          crud-table/action-column]
                :edit-route :admin.customization.menus.edit
                :entity-type :menu}]}))

(admin.skeleton/add-menu-item!
 {:route :admin.customization.menus
  :parent :admin.customization
  :mobile? false
  :label ::page})

(routes/define-route!
 :admin.customization.menus
 {:name ::page
  :url "menus"
  :component page
  :init-fx [::init]})
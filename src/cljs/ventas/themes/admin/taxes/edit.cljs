(ns ventas.themes.admin.taxes.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.crud-form :as crud-form :include-macros true]))

(def state-path [::state])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::events/enums.get :tax.kind]
                 [::crud-form/init state-path :tax]]}))

(defn content []
  (let [{{:keys [culture]} :identity} @(rf/subscribe [:db [:session]])]
    [base/segment {:color "orange"
                   :title "Tax"}
     (crud-form/field
      state-path
      {:key :tax/name
       :type :i18n
       :culture culture})

     (crud-form/field
      state-path
      {:key :tax/amount
       :type :amount})

     (crud-form/field
      state-path
      {:key [:tax/kind :db/id]
       :type :combobox
       :options @(rf/subscribe [:db [:enums :tax.kind]])})]))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-taxes-edit__page
    [crud-form/component state-path :admin.taxes
     [content]]]])

(routes/define-route!
  :admin.taxes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})

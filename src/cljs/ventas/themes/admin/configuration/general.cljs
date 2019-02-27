(ns ventas.themes.admin.configuration.general
  (:require
   [re-frame.core :as rf]
   [reagent.ratom :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.server.api.admin :as api.admin]
   [ventas.themes.admin.common :refer [entity->option]]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(def form-path [state-key])

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::api.admin/admin.general-config.set
               {:params (form/get-data db form-path)
                :success [::notificator/notify-saved]}]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw key))})])

(rf/reg-sub
 ::culture-options
 (fn [db]
   (map entity->option (get-in db [state-key :cultures]))))

(defn- content []
  [form/form [state-key]
   [base/segment {:color "orange"
                  :title (i18n ::page)}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

     [field {:key :general-config/culture
             :type :combobox
             :options @(rf/subscribe [::culture-options])}]

     [base/divider {:hidden true}]

     [base/form-button
      {:type "submit"}
      (i18n ::submit)]]]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-general-configuration__page
    [content]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::api.admin/admin.general-config.get
                  {:success [::form/populate [state-key]]}]
                 [::api.admin/admin.entities.list
                  {:params {:type :i18n.culture}
                   :success [:db [state-key :cultures]]}]]}))

(routes/define-route!
 :admin.configuration.general
 {:name ::page
  :url "general"
  :component page
  :init-fx [::init]})

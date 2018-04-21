(ns ventas.pages.admin.taxes.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.entities.save
               {:params (get-in db [state-key :form])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
    :go-to [:admin.taxes]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::events/enums.get :tax.kind]
                 (let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/tax}]
                     [::backend/admin.entities.pull
                      {:params {:id id}
                       :success [::form/populate [state-key]]}]))]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])

(defn content []
  [form/form [state-key]
   (let [{{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])]
     [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

      [base/segment {:color "orange"
                     :title "Tax"}

       [field {:key :tax/name
               :type :i18n
               :culture culture}]

       [field {:key :tax/amount
               :type :amount}]

       [field {:key [:tax/kind :db/id]
               :type :combobox
               :options @(rf/subscribe [::events/db [:enums :tax.kind]])}]]

      [base/form-button {:type "submit"}
       (i18n ::submit)]])])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-taxes-edit__page
    [content]]])

(routes/define-route!
  :admin.taxes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})

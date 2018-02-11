(ns ventas.pages.admin.taxes.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [_ [_ data]]
   {:dispatch [::backend/admin.taxes.save
               {:params data
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/add {:message (i18n ::tax-saved-notification)
                                  :theme "success"}]
    :go-to [:admin.taxes]}))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::events/enums.get :tax.kind]
                 [::backend/entities.find (routes/ref-from-param :id)
                  {:success [::form/populate state-key]}]]}))

(defn form []
  (let [form @(rf/subscribe [::events/db [state-key :form]])
        form-hash @(rf/subscribe [::events/db [state-key :form-hash]])]
    ^{:key form-hash}
    [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit form]))}

     [base/segment {:color "orange"
                    :title "Tax"}
      (let [field :name]
        [base/form-input
         {:label (i18n ::name)
          :default-value (get form field)
          :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])

      (let [field :amount]
        [base/form-input
         {:label (i18n ::amount)
          :default-value (get form field)
          :on-change #(rf/dispatch [::set-field field (js/parseFloat (-> % .-target .-value))])}])

      (let [field :kind]
        [base/form-field
         [:label (i18n ::kind)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map #(update % :value str)
                         @(rf/subscribe [::events/db [:enums :tax.kind]]))
           :default-value (str (get form field))
           :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])]

     [base/divider {:hidden true}]

     [base/form-button {:type "submit"} (i18n ::submit)]]))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-taxes-edit__page
    [form]]])

(routes/define-route!
  :admin.taxes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})

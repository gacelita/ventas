(ns ventas.pages.admin.taxes.edit
  (:require
   [re-frame.core :as rf]
   [reagent.core :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]))

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [::backend/admin.taxes.save
               {:params data
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::tax-saved-notification) :theme "success"}]
    :go-to [:admin.taxes]}))

(defn form []
  (let [data (atom {})
        key (atom nil)]
    (rf/dispatch [::backend/entities.find (routes/ref-from-param :id)
                  {:success (fn [entity]
                              (reset! data entity)
                              (reset! key (hash entity)))}])
    (rf/dispatch [::events/enums.get :tax.kind])
    (fn []
      ^{:key @key}
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit @data]))}

       [base/segment {:color "orange"
                      :title "Tax"}
        [base/form-input
         {:label (i18n ::name)
          :default-value (:name @data)
          :on-change #(swap! data assoc :name (-> % .-target .-value))}]
        [base/form-input
         {:label (i18n ::amount)
          :default-value (:amount @data)
          :on-change #(swap! data assoc :amount (js/parseFloat (-> % .-target .-value)))}]
        [base/form-field
         [:label (i18n ::kind)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map #(update % :value str)
                         @(rf/subscribe [::events/db [:enums :tax.kind]]))
           :default-value (str (:kind @data))
           :on-change #(swap! data assoc :kind (.-value %2))}]]]

       [base/divider {:hidden true}]

       [base/form-button {:type "submit"} (i18n ::submit)]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-taxes-edit__page
    [form]]])

(routes/define-route!
  :admin.taxes.edit
  {:name ::page
   :url [:id "/edit"]
   :component page})

(ns ventas.pages.admin.taxes.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [soda-ash.core :as sa]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.common.util :as common.util]
   [ventas.components.notificator :as notificator]))

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [:api/taxes.save {:params data
                                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::tax-saved-notification) :theme "success"}]
    :go-to [:admin.taxes]}))

(defn form []
  (let [data (atom {})
        key (atom nil)]
    (rf/dispatch [:api/entities.find
                  (get-in (routes/current) [:route-params :id])
                  {:success-fn (fn [entity]
                                 (reset! data entity)
                                 (reset! key (hash entity)))}])
    (rf/dispatch [:ventas/reference :tax.kind])
    (fn []
      ^{:key @key}
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit @data]))}
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
                        @(rf/subscribe [:ventas/db [:reference :tax.kind]]))
          :default-value (str (:kind @data))
          :on-change #(swap! data assoc :kind (.-value %2))}]]
       [base/form-button {:type "submit"} (i18n ::submit)]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin-taxes-edit__page
    [form]]])

(routes/define-route!
 :admin.taxes.edit
 {:name (i18n ::page)
  :url [:id "/edit"]
  :component page})
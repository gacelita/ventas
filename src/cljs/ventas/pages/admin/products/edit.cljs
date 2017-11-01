(ns ventas.pages.admin.products.edit
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
   [ventas.pages.admin :as admin]
   [ventas.i18n :refer [i18n]]
   [ventas.common.util :as common.util]
   [ventas.components.notificator :as notificator]))

(def brands-sub-key ::brands)

(def taxes-sub-key ::taxes)

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [:api/products.save {:params data
                                   :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::product-saved-notification) :theme "success"}]
    :go-to [:admin.products]}))

(defn image [eid]
  (rf/dispatch [:ventas/entities.sync eid])
  (fn [eid]
    (let [data @(rf/subscribe [:ventas/db [:entities eid]])]
      [base/image {:src (:url data)
                   :size "medium"
                   :share "rounded"}])))

(defn product-form []
  (let [data (atom {})
        key (atom nil)]
    (rf/dispatch [:api/entities.find
                  (get-in (routes/current) [:route-params :id])
                  {:success-fn (fn [entity-data]
                                 (reset! data entity-data)
                                 (reset! key (hash entity-data)))}])
    (rf/dispatch [:api/brands.list {:success-fn #(rf/dispatch [:ventas/db [brands-sub-key] %])}])
    (rf/dispatch [:api/taxes.list {:success-fn #(rf/dispatch [:ventas/db [taxes-sub-key] %])}])

    (fn []
      ^{:key @key}
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit @data]))}
       [base/form-input
        {:label (i18n ::name)
         :default-value (:name @data)
         :on-change #(swap! data assoc :name (-> % .-target .-value))}]
       [base/form-field
        [:label (i18n ::active)]
        [base/checkbox
         {:toggle true
          :checked (:active @data)
          :on-change #(swap! data assoc :active (-> % .-target .-value))}]]
       [base/form-input
        {:label (i18n ::price)
         :default-value (:price @data)
         :on-change #(swap! data assoc :price (-> % .-target .-value))}]
       [base/form-input
        {:label (i18n ::reference)
         :default-value (:reference @data)
         :on-change #(swap! data assoc :reference (-> % .-target .-value))}]
       [base/form-input
        {:label (i18n ::ean13)
         :default-value (:ean13 @data)
         :on-change #(swap! data assoc :ean13 (-> % .-target .-value))}]
       [base/form-textarea
        {:label (i18n ::description)
         :default-value (:description @data)
         :on-change #(swap! data assoc :description (-> % .-target .-value))}]
       [base/form-field
        [:label (i18n ::tags)]
        [base/dropdown
         {:allowAdditions true
          :multiple true
          :fluid true
          :search true
          :options (map (fn [v] {:text v :value v})
                        (:tags @data))
          :selection true
          :default-value (:tags @data)
          :on-change #(swap! data assoc :tags (set (map common.util/read-keyword (.-value %2))))}]]
       [base/form-field
        [:label (i18n ::brand)]
        [base/dropdown
         {:fluid true
          :selection true
          :options (map (fn [v] {:text (:name v) :value (:id v)})
                        @(rf/subscribe [brands-sub-key]))
          :default-value (:brand @data)
          :on-change #(swap! data assoc :brand (.-value %2))}]]
       [base/form-field
        [:label (i18n ::tax)]
        [base/dropdown
         {:fluid true
          :selection true
          :options (map (fn [v] {:text (:name v) :value (:id v)})
                        @(rf/subscribe [taxes-sub-key]))
          :default-value (:tax @data)
          :on-change #(swap! data assoc :tax (.-value %2))}]]
       [base/form-field
        [:label (i18n ::images)]
        [base/imageGroup
         (for [eid (:images @data)]
           [image eid])]]
       [base/form-button {:type "submit"} (i18n ::send)]])))

(defn page []
  [admin/skeleton
   [:div.admin-products-edit__page
    [product-form]]])

(routes/define-route!
 :admin.products.edit
 {:name (i18n ::page)
  :url [:id "/edit"]
  :component page})
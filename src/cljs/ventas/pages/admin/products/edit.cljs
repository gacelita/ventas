(ns ventas.pages.admin.products.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.common.util :as common.util]
   [ventas.components.notificator :as notificator]))

(def brands-sub-key ::brands)

(def taxes-sub-key ::taxes)

(def form-data-key ::form-data)

(def form-hash-key ::form-key)

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

(def image-modal-key ::image-modal)

(rf/reg-event-db
 ::image-modal.open
 (fn [db [_ url]]
   (assoc db image-modal-key {:open true
                              :url url})))

(rf/reg-event-db
 ::image-modal.close
 (fn [db [_]]
   (assoc-in db [image-modal-key :open] false)))

(defn image-modal []
  (let [{:keys [open url]} @(rf/subscribe [:ventas/db [image-modal-key]])]
    [base/modal {:basic true
                 :size "small"
                 :open open
                 :on-close #(rf/dispatch [::image-modal.close])}
     [base/modalContent {:image true}
      [base/image {:wrapped true
                   :size "large"
                   :src url}]]]))

(defn image [eid]
  (rf/dispatch [:ventas/entities.sync eid])
  (fn [eid]
    (let [data @(rf/subscribe [:ventas/db [:entities eid]])]
      [base/image {:src (:url data)
                   :size "small"
                   :on-click #(rf/dispatch [::image-modal.open (:url data)])}])))

(rf/reg-event-db
 ::set-field
 (fn [db [_ field value]]
   (assoc db form-data-key field value)))

(rf/reg-event-db
 ::update-field
 (fn [db [_ field update-fn]]
   (update-in db [form-data-key field] update-fn)))

(rf/reg-event-fx
 ::upload.next
 (fn [db [_ image]]
   {:dispatch [::update-field :images #(conj (vec %) (:db/id image))]}))

(defn- image-placeholder []
  (let [ref (atom nil)]
    (fn []
      [:div.ui.small.image.admin-products-edit__image-placeholder
       {:on-click #(-> @ref (.click))}
       [base/icon {:name "plus"}]
       [:input {:type "file"
                :ref #(reset! ref %)
                :on-change (fn [e]
                             (rf/dispatch [:ventas/upload {:success #(rf/dispatch [::upload.next %])
                                                           :file (-> (-> e .-target .-files)
                                                                     (js/Array.from)
                                                                     first)}]))}]])))

(defn product-form []
  (rf/dispatch [:api/entities.find
                (get-in (routes/current) [:route-params :id])
                {:success (fn [entity-data]
                               (rf/dispatch [:ventas/db [form-data-key] entity-data])
                               (rf/dispatch [:ventas/db [form-hash-key] (hash entity-data)]))}])
  (rf/dispatch [:api/brands.list {:success #(rf/dispatch [:ventas/db [brands-sub-key] %])}])
  (rf/dispatch [:api/taxes.list {:success #(rf/dispatch [:ventas/db [taxes-sub-key] %])}])

  (fn []
    (let [form-data @(rf/subscribe [:ventas/db [form-data-key]])
          form-hash @(rf/subscribe [:ventas/db [form-hash-key]])]
      ^{:key form-hash}
      [:div
       [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit form-data]))}
        [base/form-input
         {:label (i18n ::name)
          :default-value (:name form-data)
          :on-change #(rf/dispatch [::set-field :name (-> % .-target .-value)])}]
        [base/form-field
         [:label (i18n ::active)]
         [base/checkbox
          {:toggle true
           :checked (:active form-data)
           :on-change #(rf/dispatch [::set-field :active (-> % .-target .-value)])}]]
        [base/form-input
         {:label (i18n ::price)
          :default-value (:price form-data)
          :on-change #(rf/dispatch [::set-field :price (-> % .-target .-value)])}]
        [base/form-input
         {:label (i18n ::reference)
          :default-value (:reference form-data)
          :on-change #(rf/dispatch [::set-field :reference (-> % .-target .-value)])}]
        [base/form-input
         {:label (i18n ::ean13)
          :default-value (:ean13 form-data)
          :on-change #(rf/dispatch [::set-field :ean13 (-> % .-target .-value)])}]
        [base/form-textarea
         {:label (i18n ::description)
          :default-value (:description form-data)
          :on-change #(rf/dispatch [::set-field :description (-> % .-target .-value)])}]
        [base/form-field
         [:label (i18n ::tags)]
         [base/dropdown
          {:allowAdditions true
           :multiple true
           :fluid true
           :search true
           :options (map (fn [v] {:text v :value v})
                         (:tags form-data))
           :selection true
           :default-value (:tags form-data)
           :on-change #(rf/dispatch [::set-field :tags (set (map common.util/read-keyword (.-value %2)))])}]]
        [base/form-field
         [:label (i18n ::brand)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map (fn [v] {:text (:name v) :value (:id v)})
                         @(rf/subscribe [:ventas/db [brands-sub-key]]))
           :default-value (:brand form-data)
           :on-change #(rf/dispatch [::set-field :brand (.-value %2)])}]]
        [base/form-field
         [:label (i18n ::tax)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map (fn [v] {:text (:name v) :value (:id v)})
                         @(rf/subscribe [:ventas/db [taxes-sub-key]]))
           :default-value (:tax form-data)
           :on-change #(rf/dispatch [::set-field :tax (.-value %2)])}]]
        [base/form-field {:class "admin-products-edit__images"}
         [:label (i18n ::images)]
         [base/imageGroup
          (for [eid (:images form-data)]
            ^{:key eid} [image eid])
          [image-placeholder]]]
        [base/form-button {:type "submit"} (i18n ::send)]]
       [image-modal]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-products-edit__page
    [product-form]]])

(routes/define-route!
 :admin.products.edit
 {:name (i18n ::page)
  :url [:id "/edit"]
  :component page})
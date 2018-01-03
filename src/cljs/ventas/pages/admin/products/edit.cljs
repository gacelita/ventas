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
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(def brands-sub-key ::brands)

(def taxes-sub-key ::taxes)

(def form-data-key ::form-data)

(def form-hash-key ::form-key)

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [::backend/products.save {:params data
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
  (let [{:keys [open url]} @(rf/subscribe [::events/db [image-modal-key]])]
    [base/modal {:basic true
                 :size "small"
                 :open open
                 :on-close #(rf/dispatch [::image-modal.close])}
     [base/modal-content {:image true}
      [base/image {:wrapped true
                   :size "large"
                   :src url}]]]))

(defn image [eid]
  (rf/dispatch [::events/entities.sync eid])
  (fn [eid]
    (let [{:keys [id url]} @(rf/subscribe [::events/db [:entities eid]])]
      [base/image {:src (str "/images/" id "/resize/admin-products-edit")
                   :size "small"
                   :on-click #(rf/dispatch [::image-modal.open url])}])))

(rf/reg-event-db
 ::set-field
 (fn [db [_ field value]]
   (let [field (if-not (sequential? field)
                 [field]
                 field)]
     (assoc-in db (concat [form-data-key] field) value))))

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
                             (rf/dispatch [::events/upload
                                           {:success #(rf/dispatch [::upload.next %])
                                            :file (-> (-> e .-target .-files)
                                                      js/Array.from
                                                      first)}]))}]])))

(defn product-form []
  (rf/dispatch [::backend/entities.find
                (-> (routes/current)
                    (get-in [:route-params :id])
                    (js/parseInt))
                {:success (fn [entity-data]
                               (rf/dispatch [::events/db [form-data-key] entity-data])
                               (rf/dispatch [::events/db [form-hash-key] (hash entity-data)]))}])
  (rf/dispatch [::backend/admin.brands.list {:success #(rf/dispatch [::events/db [brands-sub-key] %])}])
  (rf/dispatch [::backend/admin.taxes.list {:success #(rf/dispatch [::events/db [taxes-sub-key] %])}])

  (fn []
    (let [form-data @(rf/subscribe [::events/db [form-data-key]])
          form-hash @(rf/subscribe [::events/db [form-hash-key]])]
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
          :default-value (get-in form-data [:price :value])
          :on-change #(rf/dispatch [::set-field [:price :value]
                                    (js/parseInt (-> % .-target .-value))])}]
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
           :default-value (or (:tags form-data) #{})
           :on-change #(rf/dispatch [::set-field :tags (set (.-value %2))])}]]
        [base/form-field
         [:label (i18n ::brand)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map (fn [v] {:text (:name v) :value (:id v)})
                         @(rf/subscribe [::events/db [brands-sub-key]]))
           :default-value (get-in form-data [:brand :id])
           :on-change #(rf/dispatch [::set-field :brand (.-value %2)])}]]
        [base/form-field
         [:label (i18n ::tax)]
         [base/dropdown
          {:fluid true
           :selection true
           :options (map (fn [v] {:text (:name v) :value (:id v)})
                         @(rf/subscribe [::events/db [taxes-sub-key]]))
           :default-value (get-in form-data [:tax :id])
           :on-change #(rf/dispatch [::set-field :tax (.-value %2)])}]]
        [base/form-field {:class "admin-products-edit__images"}
         [:label (i18n ::images)]
         [base/image-group
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
 {:name ::page
  :url [:id "/edit"]
  :component page})
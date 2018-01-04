(ns ventas.pages.admin.products.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.components.i18n-input :as i18n-input]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(def state-key ::state)

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
     (assoc-in db (concat [state-key :product] field) value))))

(rf/reg-event-db
 ::update-field
 (fn [db [_ field update-fn]]
   (let [field (if-not (sequential? field)
                 [field]
                 field)]
     (update-in db (concat [state-key :product] field) update-fn))))

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
  (rf/dispatch [::backend/admin.entities.find
                {:params {:id (-> (routes/current)
                                  (get-in [:route-params :id])
                                  (js/parseInt))}
                 :success (fn [entity-data]
                            (rf/dispatch [::events/db [state-key :product] entity-data])
                            (rf/dispatch [::events/db [state-key :product-hash] (hash entity-data)]))}])
  (rf/dispatch [::backend/admin.brands.list {:success [::events/db [state-key :brands]]}])
  (rf/dispatch [::backend/admin.taxes.list {:success [::events/db [state-key :taxes]]}])
  (rf/dispatch [::events/i18n.cultures.list])
  (fn []
    (let [product @(rf/subscribe [::events/db [state-key :product]])
          form-hash @(rf/subscribe [::events/db [state-key :product-hash]])
          {{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])]
      ^{:key form-hash}
      [:div
       [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit product]))}

        [base/segment {:color "orange"
                       :title "Product"}

         (let [field :name]
           [i18n-input/input
            {:label (i18n ::name)
             :value (get product field)
             :culture culture
             :on-change #(rf/dispatch [::set-field field %])}])
         (let [field :active]
           [base/form-field
            [:label (i18n ::active)]
            [base/checkbox
             {:toggle true
              :checked (get product field)
              :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}]])
         (let [field :reference]
           [base/form-input
            {:label (i18n ::reference)
             :default-value (get product field)
             :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])
         (let [field :ean13]
           [base/form-input
            {:label (i18n ::ean13)
             :default-value (get product field)
             :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])]

        [base/divider {:hidden true}]

        [base/segment {:color "orange"
                       :title "Price"}

         (let [field :price]
           [base/form-input
            {:label (i18n ::price)
             :default-value (get product field)
             :on-change #(rf/dispatch [::set-field
                                       field
                                       (js/parseInt (-> % .-target .-value))])}])

         (let [field :tax]
           [base/form-field
            [:label (i18n ::tax)]
            [base/dropdown
             {:fluid true
              :selection true
              :options (map (fn [v] {:text (:name v) :value (:id v)})
                            @(rf/subscribe [::events/db [state-key :taxes]]))
              :default-value (get product field)
              :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])]

        [base/divider {:hidden true}]

        [base/segment {:title "Description"
                       :color "orange"}

         (let [field :description]

           [i18n-input/input
            {:label (i18n ::description)
             :control :textarea
             :value (get product field)
             :culture culture
             :on-change #(rf/dispatch [::set-field field %])}])

         (let [field :tags]
           [base/form-field
            [:label (i18n ::tags)]
            [base/dropdown
             {:allowAdditions true
              :multiple true
              :fluid true
              :search true
              :options (map (fn [v] {:text v :value v})
                            (get product field))
              :selection true
              :default-value (or (get product field) #{})
              :on-change #(rf/dispatch [::set-field field (set (.-value %2))])}]])

         (let [field :brand]
           [base/form-field
            [:label (i18n ::brand)]
            [base/dropdown
             {:fluid true
              :selection true
              :options (map (fn [v] {:text (:name v) :value (:id v)})
                            @(rf/subscribe [::events/db [state-key :brands]]))
              :default-value (get product field)
              :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])]

        [base/divider {:hidden true}]

        [base/segment {:color "orange"
                       :title "Images"}

         (let [field :images]
           [base/form-field {:class "admin-products-edit__images"}
            [base/image-group
             (for [eid (get product field)]
               ^{:key eid} [image eid])
             [image-placeholder]]])]

        [base/divider {:hidden true}]

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
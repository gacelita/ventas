(ns ventas.pages.admin.products.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.components.i18n-input :as i18n-input]
   [ventas.components.amount-input :as amount-input]
   [ventas.components.draggable-list :as draggable-list]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [ventas.utils.logging :as log]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [cofx [_ data]]
   {:dispatch [::backend/admin.products.save
               {:params data
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

(rf/reg-event-db
 ::remove-image
 (fn [db [_ file-id]]
   (update-in db
              [state-key :product :product/images]
              (fn [images]
                (remove #(= file-id (get-in % [:product.image/file :db/id]))
                        images)))))

(defn image-view [{:product.image/keys [file]}]
  (let [{:db/keys [id]} file]
    [:div.admin-products-edit__image
     [base/image {:src (str "/images/" id "/resize/admin-products-edit")
                  :size "small"
                  :on-click (utils.ui/with-handler
                             #(rf/dispatch [::image-modal.open id]))}]
     [base/button {:icon true
                   :size "mini"
                   :on-click (utils.ui/with-handler
                              #(rf/dispatch [::remove-image id]))}
      [base/icon {:name "remove"}]]]))

(defmulti set-field-filter (fn [field _] field))

(defmethod set-field-filter :product/images [_ value]
  (->> value
       (map-indexed (fn [idx itm]
                      (assoc itm :product.image/position idx)))))

(defmethod set-field-filter :default [_ value]
  value)

(rf/reg-event-db
 ::set-field
 (fn [db [_ field value]]
   (let [field (if-not (sequential? field)
                 [field]
                 field)]
     (assoc-in db
               (concat [state-key :product] field)
               (set-field-filter field value)))))

(rf/reg-event-fx
 ::update-field
 (fn [{:keys [db]} [_ field update-fn]]
   (let [field (if-not (sequential? field)
                 [field]
                 field)
         new-value (update-fn (get-in db (concat [state-key :product] field)))]
     {:dispatch [::set-field field new-value]})))

(rf/reg-event-fx
 ::upload
 (fn [cofx [_ file]]
   {:dispatch [::events/upload
               {:success ::upload.next
                :file file}]}))

(rf/reg-event-fx
 ::upload.next
 (fn [db [_ {:db/keys [id]}]]
   (let [image {:schema/type :schema.type/product.image
                :product.image/file {:db/id id}}]
     {:dispatch [::update-field
                 :product/images
                 #(conj (vec %) image)]})))

(defn- image-placeholder []
  (let [ref (atom nil)]
    (fn []
      [:div.ui.small.image.admin-products-edit__image-placeholder
       {:on-click #(-> @ref (.click))}
       [base/icon {:name "plus"}]
       [:input {:type "file"
                :ref #(reset! ref %)
                :on-change #(rf/dispatch [::upload (-> (-> % .-target .-files)
                                                       js/Array.from
                                                       first)])}]])))

(rf/reg-event-db
 ::set-product
 (fn [db [_ product]]
   (-> db
       (assoc-in [state-key :product] product)
       (assoc-in [state-key :product-hash] (hash product)))))

(defn product-form []
  (let [id (-> (routes/current)
               (get-in [:route-params :id])
               (js/parseInt))]
    (if-not (pos? id)
      (rf/dispatch [::set-product {:schema/type :schema.type/product}])
      (rf/dispatch [::backend/admin.entities.pull
                    {:params {:id id}
                     :success ::set-product}])))

  (rf/dispatch [::backend/admin.product.terms.list
                {:success [::events/db [state-key :product.terms]]}])
  (fn []
    (let [product @(rf/subscribe [::events/db [state-key :product]])
          form-hash @(rf/subscribe [::events/db [state-key :product-hash]])
          {{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])]
      ^{:key form-hash}
      [:div
       [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit product]))}

        (log/debug "Current product:" product)

        [base/segment {:color "orange"
                       :title "Product"}

         (let [field :product/name]
           [i18n-input/input
            {:label (i18n ::name)
             :entity (get product field)
             :culture culture
             :on-change #(rf/dispatch [::set-field field %])}])
         (let [field :product/active]
           [base/form-field
            [:label (i18n ::active)]
            [base/checkbox
             {:toggle true
              :checked (get product field)
              :on-change #(rf/dispatch [::set-field field (.-checked %2)])}]])
         (let [field :product/reference]
           [base/form-input
            {:label (i18n ::reference)
             :default-value (get product field)
             :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])
         (let [field :product/ean13]
           [base/form-input
            {:label (i18n ::ean13)
             :default-value (get product field)
             :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])]

        [base/divider {:hidden true}]

        [base/segment {:color "orange"
                       :title "Price"}

         (let [field :product/price]
           [amount-input/input
            {:label (i18n ::price)
             :amount (get product field)
             :on-change #(rf/dispatch [::set-field field %])}])

         (let [field :product/tax]
           [base/form-field
            [:label (i18n ::tax)]
            [base/dropdown
             {:fluid true
              :selection true
              :options (map (fn [v] {:text (:name v) :value (:id v)})
                            @(rf/subscribe [::events/db [:admin :taxes]]))
              :default-value (get-in product [field :db/id])
              :on-change #(rf/dispatch [::set-field [field :db/id] (.-value %2)])}]])]

        [base/divider {:hidden true}]

        [base/segment {:title "Description"
                       :color "orange"}

         (let [field :product/description]

           [i18n-input/input
            {:label (i18n ::description)
             :control :textarea
             :entity (get product field)
             :culture culture
             :on-change #(rf/dispatch [::set-field field %])}])

         (let [field :product/tags]
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
              :default-value (->> (get product field)
                                  (map :db/id)
                                  (set))
              :on-change #(rf/dispatch [::set-field field (set (.-value %2))])}]])

         (let [field :product/brand]
           [base/form-field
            [:label (i18n ::brand)]
            [base/dropdown
             {:fluid true
              :selection true
              :options (map (fn [v] {:text (:name v) :value (:id v)})
                            @(rf/subscribe [::events/db [:admin :brands]]))
              :default-value (get-in product [field :db/id])
              :on-change #(rf/dispatch [::set-field [field :db/id] (.-value %2)])}]])]

        [base/divider {:hidden true}]

        [base/segment {:color "orange"
                       :title "Images"}

         (let [field :product/images]
           [base/form-field {:class "admin-products-edit__images"}
            [base/image-group
             (let [s (reagent/atom {})]
               [:div
                [draggable-list/main-view
                 {:on-reorder (fn [items]
                                (let [images (map second items)]
                                  (rf/dispatch [::set-field field images])))}
                 (for [{:db/keys [id] :as image} (get product field)]
                   ^{:key id} [image-view image])]
                [image-placeholder]])]])]

        [base/segment {:color "orange"
                       :title "Terms"}
         (let [field :product/variation-terms]
           [base/form-field
            [:label (i18n ::variation-terms)]
            [base/dropdown
             {:multiple true
              :fluid true
              :search true
              :options (->> @(rf/subscribe [::events/db [state-key :product.terms]])
                            (map (fn [v]
                                   {:text (str (get-in v [:taxonomy :name]) ": " (:name v))
                                    :value (:id v)}))
                            (sort-by :text))
              :selection true
              :default-value (->> (get product field)
                                  (map :db/id)
                                  (set))
              :on-change #(rf/dispatch [::set-field field (->> (.-value %2)
                                                               (map js/parseInt)
                                                               (set))])}]])
         (let [field :product/terms]
           [base/form-field
            [:label (i18n ::terms)]
            [base/dropdown
             {:multiple true
              :fluid true
              :search true
              :options (->> @(rf/subscribe [::events/db [state-key :product.terms]])
                            (map (fn [v]
                                   {:text (str (get-in v [:taxonomy :name]) ": " (:name v))
                                    :value (:id v)}))
                            (sort-by :text))
              :selection true
              :default-value (->> (get product field)
                                  (map :db/id)
                                  (set))
              :on-change #(rf/dispatch [::set-field field (->> (.-value %2)
                                                               (map js/parseInt)
                                                               (set))])}]])]

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
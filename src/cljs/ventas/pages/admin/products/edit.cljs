(ns ventas.pages.admin.products.edit
  (:require
   [re-frame.core :as rf]
   [reagent.core :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.draggable-list :as draggable-list]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.common.utils :as common.utils])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.entities.save
               {:params (-> (get-in db [state-key :form])
                            (update-in [:product/price :amount/value] common.utils/str->bigdec))
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
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
              [state-key :form :product/images]
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

(rf/reg-event-fx
 ::upload
 (fn [_ [_ file]]
   {:dispatch [::events/upload
               {:success ::upload.next
                :file file}]}))

(rf/reg-event-fx
 ::upload.next
 (fn [db [_ {:db/keys [id]}]]
   (let [image {:schema/type :schema.type/product.image
                :product.image/file {:db/id id}}]
     {:dispatch [::form/update-field
                 [state-key]
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

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::backend/admin.entities.list
                  {:params {:type :product.term}
                   :success [::events/db [state-key :product.terms]]}]
                 (let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/product}]
                     [::backend/admin.entities.pull
                      {:params {:id id}
                       :success [::form/populate [state-key]]}]))]}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])

(defn product-form []
  [form/form [state-key]
   (let [{{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])
         form @(rf/subscribe [::form/data [state-key]])]
     [:div
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

       [base/segment {:color "orange"
                      :title "Product"}

        [field {:key :product/name
                :type :i18n
                :culture culture}]

        [field {:key :product/active
                :type :toggle}]

        [field {:key :product/reference}]

        [field {:key :product/ean13}]]

       [base/divider {:hidden true}]

       [base/segment {:color "orange"
                      :title "Price"}

        [field {:key :product/price
                :type :amount}]

        [field {:key [:product/tax :db/id]
                :type :combobox
                :options (map (fn [{:keys [name id]}]
                                {:text name
                                 :value id})
                              @(rf/subscribe [::events/db [:admin :taxes]]))}]]

       [base/divider {:hidden true}]

       [base/segment {:title "Description"
                      :color "orange"}

        [field {:key :product/description
                :type :i18n-textarea
                :culture culture}]

        [field {:key [:product/brand :db/id]
                :type :combobox
                :options (map (fn [{:keys [name id]}]
                                {:text name
                                 :value id})
                              @(rf/subscribe [::events/db [:admin :brands]]))}]]

       [base/divider {:hidden true}]

       [base/segment {:color "orange"
                      :title "Images"}

        (let [field :product/images]
          [base/form-field {:class "admin-products-edit__images"}
           [base/image-group
            [draggable-list/main-view
             {:on-reorder (fn [items]
                            (let [images (map second items)]
                              (rf/dispatch [::form/set-field [state-key] field images])))}
             (for [{:db/keys [id] :as image} (get form field)]
               ^{:key id} [image-view image])]
            [image-placeholder]]])]

       [base/segment {:color "orange"
                      :title "Terms"}

        [field {:key :product/variation-terms
                :type :tags
                :xform {:in #(map :db/id %)
                        :out #(map (fn [v] {:db/id v}) %)}
                :options (->> @(rf/subscribe [::events/db [state-key :product.terms]])
                              (map (fn [v]
                                     {:value (:id v)
                                      :text (str (get-in v [:taxonomy :name]) ": " (:name v))}))
                              (sort-by :text))
                :forbid-additions true}]

        [field {:key :product/terms
                :type :tags
                :xform {:in #(map :db/id %)
                        :out #(map (fn [v] {:db/id v}) %)}
                :forbid-additions true
                :options (->> @(rf/subscribe [::events/db [state-key :product.terms]])
                              (map (fn [v]
                                     {:text (str (get-in v [:taxonomy :name]) ": " (:name v))
                                      :value (:id v)}))
                              (sort-by :text))}]]

       [base/divider {:hidden true}]

       [base/form-button {:type "submit"} (i18n ::send)]]

      [image-modal]])])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-products-edit__page
    [product-form]]])

(routes/define-route!
  :admin.products.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})

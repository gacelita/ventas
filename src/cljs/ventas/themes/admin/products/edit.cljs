(ns ventas.themes.admin.products.edit
  (:require
    [re-frame.core :as rf]
    [reagent.core :refer [atom]]
    [ventas.components.base :as base]
    [ventas.components.draggable-list :as draggable-list]
    [ventas.components.form :as form]
    [ventas.components.notificator :as notificator]
    [ventas.components.image-input :as image-input]
    [ventas.server.api :as backend]
    [ventas.server.api.admin :as api.admin]
    [ventas.i18n :refer [i18n]]
    [ventas.themes.admin.skeleton :as admin.skeleton]
    [ventas.routes :as routes]
    [ventas.utils.ui :as utils.ui]
    [clojure.set :as set]
    [ventas.common.utils :as common.utils])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(def product-form-path [state-key :product])

(def variations-form-path [state-key :variations])

(defn- ->variations [db product]
  (let [variations (-> (form/get-data db variations-form-path) :variations vals)]
    (map (fn [variation]
           {:schema/type             :schema.type/product.variation
            :product/price           (-> (:product/price product)
                                         (dissoc :db/id)
                                         (assoc :amount/value (common.utils/str->bigdec (:price variation))))
            :product/reference       (:reference variation)
            :product.variation/terms (vals (dissoc variation :price :reference))})
         variations)))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   (let [product (form/get-data db product-form-path)]
     {:dispatch [::api.admin/admin.products.save
                 {:params  {:product    product
                            :variations (->variations db product)}
                  :success ::submit.next}]})))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
    :go-to [:admin.products]}))

(defmulti set-field-filter (fn [field _] field))

(defmethod set-field-filter :product/images [_ value]
  (->> value
       (map-indexed (fn [idx itm]
                      (assoc itm :product.image/position idx)))))

(defmethod set-field-filter :default [_ value]
  value)

(rf/reg-event-fx
 ::upload.next
 (fn [db [_ id]]
   (let [image {:schema/type :schema.type/product.image
                :product.image/file {:db/id id}}]
     {:dispatch [::form/update-field
                 product-form-path
                 :product/images
                 #(conj (vec %) image)]})))

(rf/reg-event-db
 ::remove-image
 (fn [db [_ file-id]]
   (update-in db
              (into product-form-path [:form :product/images])
              (fn [images]
                (remove #(= file-id (get-in % [:product.image/file :db/id]))
                        images)))))

(defn image-view [{:product.image/keys [file]}]
  (let [{:db/keys [id]} file]
    [:div.admin-products-edit__image
     [image-input/image-view {:id id
                              :on-remove [::remove-image id]}]]))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (assoc db state-key {})
    :dispatch-n [[::api.admin/admin.entities.list
                  {:params {:type :product.term}
                   :success [:db [state-key :product.terms]]}]
                 [::api.admin/admin.entities.list
                  {:params {:type :product.taxonomy}
                   :success [:db [state-key :product.taxonomies]]}]
                 [::backend/categories.options
                  {:success [:db [state-key :categories]]}]
                 (let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate product-form-path {:schema/type :schema.type/product}]
                     [::api.admin/admin.entities.pull
                      {:params {:id id}
                       :success [::form/populate product-form-path]}]))]}))

(defn- product-field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path product-form-path
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])
(rf/reg-sub
  ::term-options
  (fn [db]
    (->> (get-in db [state-key :product.terms])
         (map (fn [v]
                {:value (:id v)
                 :taxonomy (get-in v [:taxonomy :id])
                 :text (str (get-in v [:taxonomy :name]) ": " (:name v))}))
         (sort-by :text))))

(rf/reg-sub
  ::taxonomy-options
  (fn [db]
    (->> (get-in db [state-key :product.taxonomies])
         (map #(set/rename-keys % {:id :value
                                   :name :text})))))

(rf/reg-sub
  ::available-taxonomies
  :<- [::taxonomy-options]
  :<- [::form/data variations-form-path]
  (fn [[taxonomies form-data]]
    (let [used? (->> form-data :taxonomies vals set)]
      (->> taxonomies
           (remove (comp used? :value))))))

(defn- options-with-selected [available all selected-value]
  (if (or (not selected-value) (contains? (set (map :value available)) selected-value))
    available
    (conj available (->> all (filter #(= (:value %) selected-value)) (first)))))

(defn- variations-view []
  (let [available-taxonomies @(rf/subscribe [::available-taxonomies])
        all-taxonomies @(rf/subscribe [::taxonomy-options])
        all-terms @(rf/subscribe [::term-options])
        form-data @(rf/subscribe [::form/data variations-form-path])
        n-tax-columns (cond-> (-> form-data :taxonomies keys count)
                              (seq available-taxonomies) (inc))
        n-variations (-> form-data :variations keys count inc)]
    [base/segment {:color "orange"
                   :title "Variations"}
     [form/form variations-form-path
      [base/table {:celled true :class "admin-products-edit__variations"}
       [base/table-header
        [base/table-row
         [base/table-header-cell "Price"]
         [base/table-header-cell "Ref"]
         (doall
           (for [idx (range n-tax-columns)]
             (let [key [:taxonomies idx]
                   value (get-in form-data key)
                   options (options-with-selected available-taxonomies all-taxonomies value)]
               [base/table-header-cell
                [form/field {:db-path     variations-form-path
                             :type        :combobox
                             :options     options
                             :key         key
                             :placeholder "Taxonomy"}]])))]]
       [base/table-body
        (doall
          (for [idx (range n-variations)]
            [base/table-row {:key idx}
             [base/table-cell
              [form/field {:db-path     variations-form-path
                           :type        :text
                           :key         [:variations idx :price]
                           :placeholder "Price"}]]
             [base/table-cell
              [form/field {:db-path     variations-form-path
                           :type        :text
                           :key         [:variations idx :reference]
                           :placeholder "Ref"}]]
             (map (fn [[_ taxonomy]]
                    [base/table-cell
                     [form/field {:db-path     variations-form-path
                                  :type        :combobox
                                  :options     (filter #(= (:taxonomy %) taxonomy) all-terms)
                                  :key         [:variations idx taxonomy]
                                  :placeholder "Term"}]])
                  (:taxonomies form-data))]))]]]]))

(defn- terms-view []
  [base/segment {:color "orange"
                 :title "Terms"}

   [product-field {:key      :product/variation-terms
           :type             :tags
           :xform            {:in #(map :db/id %)
                   :out #(map (fn [v] {:db/id v}) %)}
           :options          @(rf/subscribe [::term-options])
           :forbid-additions true}]

   [product-field {:key      :product/terms
           :type             :tags
           :xform            {:in #(map :db/id %)
                   :out #(map (fn [v] {:db/id v}) %)}
           :forbid-additions true
           :options          @(rf/subscribe [::term-options])}]])

(defn- images-view [form]
  [base/segment {:color "orange"
                 :title "Images"}

   (let [field :product/images]
     [base/form-field {:class "admin-products-edit__images"}
      [base/image-group
       [draggable-list/main-view
        {:on-reorder (fn [items]
                       (let [images (map second items)]
                         (rf/dispatch [::form/set-field product-form-path field images])))}
        (for [{:db/keys [id] :as image} (get form field)]
          ^{:key id} [image-view image])]
       [image-input/image-placeholder
        {:on-change [::upload.next]}]]])])

(defn- description-view []
  [base/segment {:title "Description"
                 :color "orange"}

   [product-field {:key  :product/description
                   :type :i18n-textarea}]

   [product-field {:key [:product/brand :db/id]
           :type        :combobox
           :options     (map (fn [{:keys [name id]}]
                           {:text name
                            :value id})
                         @(rf/subscribe [:db [:admin :brands]]))}]])

(defn- categories-view []
  [base/segment {:color "orange"
                 :title "Categories"}
   [product-field {:key      :product/categories
           :type             :tags
           :xform            {:in #(map :db/id %)
                   :out #(map (fn [v] {:db/id v}) %)}
           :options          (->> @(rf/subscribe [:db [state-key :categories]])
                         (map (fn [v]
                                {:text (val v)
                                 :value (key v)}))
                         (sort-by :text))
           :forbid-additions true}]])

(defn- price-view []
  [base/segment {:color "orange"
                 :title "Price"}

   [product-field {:key :product/price
           :type        :amount}]

   [product-field {:key [:product/tax :db/id]
           :type        :combobox
           :options     (map (fn [{:keys [name id]}]
                           {:text name
                            :value id})
                         @(rf/subscribe [:db [:admin :taxes]]))}]])

(defn- meta-view [culture]
  [base/segment {:color "orange"
                 :title "Product"}

   [product-field {:key  :product/name
                   :type :i18n}]

   [product-field {:key  :product/active
                   :type :toggle}]

   [product-field {:key :product/reference}]

   [product-field {:key :product/ean13}]])

(defn product-form []
  [form/form product-form-path
   (let [{{:keys [culture]} :identity} @(rf/subscribe [:db [:session]])
         form @(rf/subscribe [::form/data product-form-path])]
     [:div
      [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}
       [meta-view culture]
       [base/divider {:hidden true}]
       [price-view]
       [base/divider {:hidden true}]
       [description-view culture]
       [base/divider {:hidden true}]
       [product-field {:key  :product/images
                       :type ::image-input/image-list}]
       [base/divider {:hidden true}]
       [terms-view]
       [base/divider {:hidden true}]
       [variations-view]
       [base/divider {:hidden true}]
       [categories-view]
       [base/divider {:hidden true}]
       [base/form-button {:type "submit"} (i18n ::send)]]
      [image-input/image-modal]])])

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

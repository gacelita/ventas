(ns ventas.components.product-filters
  (:require
   [ventas.components.base :as base]
   [ventas.components.sidebar :as sidebar]
   [re-frame.core :as rf]))

(def mock-data
  {:products-path [::products]
   :taxonomies [{:id 1
                 :name "Color"
                 :keyword :color
                 :terms [{:id 2 :name "Color 1" :color "#000" :count 2}
                         {:id 3 :name "Color 2" :color "#fff" :count 2}
                         {:id 4 :name "Color 3" :color "#aaa" :count 2}
                         {:id 5 :name "Color 4" :color "#ccc" :count 2}
                         {:id 6 :name "Color 5" :color "#eee" :count 2}
                         {:id 7 :name "Color 1" :color "#000" :count 2}
                         {:id 8 :name "Color 2" :color "#fff" :count 2}
                         {:id 9 :name "Color 3" :color "#aaa" :count 2}
                         {:id 10 :name "Color 4" :color "#ccc" :count 2}
                         {:id 11 :name "Color 5" :color "#eee" :count 2}
                         {:id 12 :name "Color 4" :color "#ccc" :count 2}
                         {:id 13 :name "Color 5" :color "#eee" :count 2}]}
                {:id 2
                 :name "Marca"
                 :keyword :brand
                 :terms [{:id 2 :name "Marca 1" :count 2}
                         {:id 3 :name "Marca 2" :count 2}
                         {:id 4 :name "Marca 3" :count 2}
                         {:id 5 :name "Marca 4" :count 2}
                         {:id 6 :name "Marca 5" :count 2}]}
                {:id 3
                 :name "Talla"
                 :keyword :size
                 :terms [{:id 10 :name "S" :count 2}
                         {:id 11 :name "M" :count 2}
                         {:id 12 :name "L" :count 2}
                         {:id 13 :name "XL" :count 2}
                         {:id 14 :name "XXL" :count 2}]}
                {:id 4
                 :name "Categoría"
                 :keyword :size
                 :terms [{:id 10 :name "S" :count 2}
                         {:id 11 :name "M" :count 2}
                         {:id 12 :name "L" :count 2}
                         {:id 13 :name "XL" :count 2}
                         {:id 14 :name "XXL" :count 2}]}]})

(def filters-key ::product-filters)

(defn state->api-params [taxonomies]
  (set
   (mapcat (fn [[taxonomy-id {:keys [selection]}]]
             selection)
           taxonomies)))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} [_ products-path]]
   {:db (assoc-in db products-path {:products []
                                    :pagination {:items-per-page 4
                                                 :page 0}})
    :dispatch [::apply-filters products-path]}))

(rf/reg-event-fx
 ::apply-filters
 (fn [{:keys [db]} [_ products-path]]
   {:dispatch [:api/products.list
               {:params {:filters {:terms (state->api-params (get db filters-key))}
                         :pagination (:pagination (get-in db products-path))}
                :success-fn #(rf/dispatch [::apply-filters.next products-path %])}]}))

(rf/reg-event-db
 ::apply-filters.next
 (fn [db [_ products-path products]]
   (let [state (get-in db products-path)]
     (-> db
         (update-in (conj products-path :products) #(concat % products))
         (update-in (concat products-path [:pagination :page]) inc)))))

(rf/reg-event-fx
 ::add-filter
 (fn [{:keys [db]} [_ {:keys [products-path taxonomy-id term-id term-value]}]]
   {:db (update-in db
                   [filters-key taxonomy-id :selection]
                   #(if term-value (conj (set %) term-id)
                                   (disj (set %) term-id)))
    :dispatch [::apply-filters products-path]}))

(defmulti product-term* (fn [products-path {:keys [keyword]} term] keyword))

(defmethod product-term* :color [products-path taxonomy term]
  [:div {:title (:name term)}])

(defmethod product-term* :category [products-path taxonomy term]
  [:div (:name term)])

(defmethod product-term* :default [products-path taxonomy term]
  [base/checkbox {:label (:name term)
                  :on-change #(rf/dispatch [::add-filter {:products-path products-path
                                                          :taxonomy-id (:id taxonomy)
                                                          :term-id (:id term)
                                                          :term-value (.-checked %2)}])}])

(defn product-term [products-path {:keys [keyword] :as taxonomy} term]
  [:div.product-filter__term {:class (str "product-filter__term--" (name keyword))}
   [product-term* products-path taxonomy term]])

(defn product-filters [{:keys [products-path]}]
  (rf/dispatch [::init products-path])
  (fn [{:keys [taxonomies products-path]}]
    (assert (coll? products-path))
    [sidebar/sidebar
     [:div.product-filters
      (for [taxonomy taxonomies]
        ^{:key (:id taxonomy)}
        [sidebar/sidebar-section {:id (:id taxonomy)
                                  :name (:name taxonomy)}
         (for [term (:terms taxonomy)]
           [product-term products-path taxonomy term])])]]))
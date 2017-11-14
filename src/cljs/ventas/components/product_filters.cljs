(ns ventas.components.product-filters
  (:require [ventas.components.base :as base]
            [re-frame.core :as rf]))

(def mock-data
  [{:taxonomy {:id 1 :name "Color" :keyword :color}
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
   {:taxonomy {:id 2 :name "Marca" :keyword :brand}
    :terms [{:id 2 :name "Marca 1" :count 2}
            {:id 3 :name "Marca 2" :count 2}
            {:id 4 :name "Marca 3" :count 2}
            {:id 5 :name "Marca 4" :count 2}
            {:id 6 :name "Marca 5" :count 2}]}
   {:taxonomy {:id 3 :name "Talla" :keyword :size}
    :terms [{:id 10 :name "S" :count 2}
            {:id 11 :name "M" :count 2}
            {:id 12 :name "L" :count 2}
            {:id 13 :name "XL" :count 2}
            {:id 14 :name "XXL" :count 2}]}
   {:taxonomy {:id 4 :name "Categoría" :keyword :size}
    :terms [{:id 10 :name "S" :count 2}
            {:id 11 :name "M" :count 2}
            {:id 12 :name "L" :count 2}
            {:id 13 :name "XL" :count 2}
            {:id 14 :name "XXL" :count 2}]}])

(defmulti product-term* (fn [taxonomy-keyword _] taxonomy-keyword))

(defmethod product-term* :color [_ {:keys [id name count color]}]
  [:div {:title name}])

(defmethod product-term* :category [_ {:keys [id name count]}]
  [:div name])

(defmethod product-term* :default [_ {:keys [id name count]}]
  [base/checkbox {:label name}])

(defn product-term [taxonomy-keyword term]
  [:div.product-filter__term {:class (str "product-filter__term--" (name taxonomy-keyword))}
   [product-term* taxonomy-keyword term]])

(def filters-key ::product-filters)

(rf/reg-event-db
 ::toggle-filter
 (fn [db [_ taxonomy-id]]
   (update-in db [filters-key taxonomy-id :closed] not)))

(defn product-filter [{:keys [taxonomy terms]}]
  (let [{:keys [closed]} @(rf/subscribe [:ventas/db [filters-key (:id taxonomy)]])]
    [:div.product-filter {:class (if closed "product-filter--closed" "product-filter--open")}
     [:div.product-filter__header
      {:on-click #(rf/dispatch [::toggle-filter (:id taxonomy)])}
      [:h2 (:name taxonomy)]
      [base/icon {:name (str "chevron " (if closed "down" "up"))}]]
     [:div.product-filter__content
      (for [term terms]
        [product-term (:keyword taxonomy) term])]]))

(defn product-filters [filters]
  (let [filters mock-data]
    [:div.product-filters
     (for [{:keys [taxonomy] :as filter} filters]
       ^{:key (:id taxonomy)}
       [product-filter filter])]))
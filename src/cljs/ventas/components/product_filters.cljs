(ns ventas.components.product-filters
  (:require
   [ventas.components.base :as base]
   [ventas.components.sidebar :as sidebar]
   [ventas.events :as events]
   [ventas.utils :as utils :include-macros true]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [ventas.events.backend :as backend]
   [ventas.common.utils :as common.utils]))

(defmulti product-term* (fn [taxonomy-kw _] taxonomy-kw))

(defmethod product-term* :color [_ {:keys [name color]}]
  [:div {:title name
         :style {:background-color color}}])

(rf/reg-event-fx
 ::add-term
 (fn [_ [_ term event]]
   {:dispatch [event (fn [filters]
                       (update filters :terms #(conj % term)))]}))

(defmethod product-term* :default [_ {:keys [name id count]} event]
  [base/checkbox {:label (str name " (" count ")")
                  :on-change #(if (.-checked %2)
                                (rf/dispatch [::add-term id event])
                                (rf/dispatch [::remove-term id event]))}])

(defn product-term [taxonomy-kw term event]
  [:div.product-filter__term {:class (when taxonomy-kw
                                       (str "product-filter__term--" (name taxonomy-kw)))}
   [product-term* taxonomy-kw term event]])

(defn- ns-kw [a]
  (utils/ns-kw a))

(defn- category-term [{:keys [category children terms event]}]
  (when-let [count (get-in terms [(:id category) :count])]
    [:div.category-term
     [:span (:name category) " (" count ")"]
     (for [[subcategory subchildren] children]
       [category-term {:category subcategory
                       :children subchildren
                       :event event
                       :terms terms}])]))

(defn- categories-view [{:keys [filters terms event]}]
  (let [terms (common.utils/index-by :id terms)
        categories @(rf/subscribe [::events/db :categories])]
    [sidebar/sidebar-section {:name ::category}
     (for [[category children] (common.utils/tree-by :id :parent categories)]
       [category-term
        {:category category
         :children children
         :terms terms
         :event event}])]))

(defn product-filters [{:keys [filters taxonomies event]}]
  (rf/dispatch [::events/categories.list])
  (fn [{:keys [filters taxonomies event]}]
    [sidebar/sidebar
     [:div.product-filters
      [categories-view {:filters filters
                        :terms (->> taxonomies
                                    (filter #(= (get-in % [:taxonomy :keyword])))
                                    first
                                    :terms)}]
      (for [{{:keys [id name keyword]} :taxonomy terms :terms} taxonomies]
        (when (not= keyword :category)
          [sidebar/sidebar-section {:key id
                                    :id id
                                    :name (or name
                                              (i18n (ns-kw keyword)))}
           (for [term terms]
             [product-term keyword term event])]))]]))
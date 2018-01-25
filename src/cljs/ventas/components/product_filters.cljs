(ns ventas.components.product-filters
  (:require
   [ventas.components.base :as base]
   [ventas.components.sidebar :as sidebar]
   [ventas.events :as events]
   [ventas.utils :as utils :include-macros true]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [ventas.events.backend :as backend]
   [ventas.common.utils :as common.utils]
   [ventas.routes :as routes]))

(defmulti product-term* (fn [taxonomy-kw _] taxonomy-kw))

(defmethod product-term* :color [_ {:keys [name color]}]
  [:div {:title name
         :style {:background-color color}}])

(rf/reg-event-fx
 ::add-term
 (fn [_ [_ term event]]
   {:dispatch [event (fn [filters]
                       (update filters :terms #(conj (set %) term)))]}))

(rf/reg-event-fx
 ::remove-term
 (fn [_ [_ term event]]
   {:dispatch [event (fn [filters]
                       (update filters :terms #(disj (set %) term)))]}))

(defmethod product-term* :default [_ {:keys [name id count]} {:keys [filters event]}]
  {:pre [event]}
  [base/checkbox {:label (str name " (" count ")")
                  :checked (contains? (set (:terms filters)) id)
                  :on-change #(if (.-checked %2)
                                (rf/dispatch [::add-term id event])
                                (rf/dispatch [::remove-term id event]))}])

(defn product-term [taxonomy-kw term {:keys [filters event]}]
  [:div.product-filter__term {:class (when taxonomy-kw
                                       (str "product-filter__term--" (name taxonomy-kw)))}
   [product-term* taxonomy-kw term {:filters filters
                                    :event event}]])

(defn- ns-kw [a]
  (utils/ns-kw a))

(defn- category-term [{:keys [category children terms event current outside-branch?]}]
  (let [count (get-in terms [(:id category) :count])
        current? (= current (:slug category))]
    (when (or outside-branch? count)
      [:div.category-term
       [:a {:href (routes/path-for :frontend.category :id (:slug category))
            :class (when current?
                     "category-term--active")}
        (str (:name category)
             (when count (str " (" count ")")))]
       (for [[subcategory subchildren] children]
         [category-term {:category subcategory
                         :children subchildren
                         :event event
                         :terms terms
                         :current current
                         :outside-branch? (if current? false outside-branch?)}])])))

(defn- categories-view [{:keys [filters terms event]}]
  (let [terms (common.utils/index-by :id terms)
        categories @(rf/subscribe [::events/db :categories])]
    [sidebar/sidebar-section {:name (i18n ::category)}
     (for [[category children] (common.utils/tree-by :id :parent categories)]
       [category-term
        {:category category
         :children children
         :terms terms
         :event event
         :current (first (:categories filters))
         :outside-branch? true}])]))

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
             [product-term keyword term {:filters filters
                                         :event event}])]))]]))
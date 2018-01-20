(ns ventas.components.product-filters
  (:require
   [ventas.components.base :as base]
   [ventas.components.sidebar :as sidebar]
   [re-frame.core :as rf]
   [ventas.events.backend :as backend]))

(defmulti product-term* (fn [taxonomy-kw _] taxonomy-kw))

(defmethod product-term* :color [_ {:keys [name color]}]
  [:div {:title name
         :style {:background-color color}}])

(defmethod product-term* :category [_ {:keys [name]}]
  [:div name])

(defmethod product-term* :default [_ {:keys [name id]} event]
  [base/checkbox {:label name
                  :on-change #(if (.-checked %2)
                                (rf/dispatch [::add-filter :term id event])
                                (rf/dispatch [::remove-filter :term id event]))}])

(defn product-term [taxonomy-kw term event]
  [:div.product-filter__term {:class (when taxonomy-kw
                                       (str "product-filter__term--" (name taxonomy-kw)))}
   [product-term* taxonomy-kw term event]])

(defn product-filters [{:keys [filters taxonomies event]}]
  (js/console.log {:filters filters
                   :taxonomies taxonomies
                   :event event})
  [sidebar/sidebar
   [:div.product-filters
    (for [{{:keys [id name keyword]} :taxonomy terms :terms} taxonomies]
      ^{:key id}
      [sidebar/sidebar-section {:id id
                                :name name}
       (for [term terms]
         [product-term keyword term event])])]])
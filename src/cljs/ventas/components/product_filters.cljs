(ns ventas.components.product-filters
  (:require
   [ventas.components.base :as base]
   [ventas.components.sidebar :as sidebar]
   [ventas.utils :as utils :include-macros true]
   [ventas.i18n :refer [i18n]]
   [re-frame.core :as rf]
   [ventas.events.backend :as backend]))

(defmulti product-term* (fn [taxonomy-kw _] taxonomy-kw))

(defmethod product-term* :color [_ {:keys [name color]}]
  [:div {:title name
         :style {:background-color color}}])

(defmethod product-term* :category [_ {:keys [name]}]
  [:div name])

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

(defn product-filters [{:keys [filters taxonomies event]}]
  (js/console.log {:filters filters
                   :taxonomies taxonomies
                   :event event})
  [sidebar/sidebar
   [:div.product-filters
    (for [{{:keys [id name keyword]} :taxonomy terms :terms} taxonomies]
      ^{:key id}
      [sidebar/sidebar-section {:id id
                                :name (or name
                                          (i18n (ns-kw keyword)))}
       (for [term terms]
         [product-term keyword term event])])]])
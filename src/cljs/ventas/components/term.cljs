(ns ventas.components.term)

(defmulti term-view (fn [taxonomy-kw _ _] taxonomy-kw))

(defmethod term-view :color [_ {:keys [color name] :as term} {:keys [on-click active?]}]

  [:div.term.term--color
   {:title name
    :style {:background-color color}
    :class (when active? "term--active")
    :on-click on-click}])

(defmethod term-view :default [_ {:keys [name] :as term} {:keys [active? on-click]}]
  [:div.term.term--default
   {:class (when active? "term--active")
    :on-click on-click}
   [:h3 name]])

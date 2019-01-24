(ns ventas.components.draggable-list
  (:require
   [re-frame.core :as rf]
   [ventas.utils.ui :as utils.ui]))

(def state-key ::state)

(defn- put-before [items pos item]
  (let [items (remove #{item} items)
        head (take pos items)
        tail (drop pos items)]
    (concat head [item] tail)))

(rf/reg-event-db
 ::on-drag-over
 (fn [db [_ id order position]]
   (let [{:keys [drag-index]} (get-in db [state-key id])]
     (if-not drag-index
       db
       (-> db
           (assoc-in [state-key id :hover-index] position)
           (assoc-in [state-key id :temp-order] (put-before order position drag-index)))))))

(defn main-view [{:keys [id]} _]
  (let [id (or id (gensym))]
    (fn [{:keys [on-reorder on-drag-over props-fn]} items]
      (let [{:keys [temp-order drag-index hover-index]} @(rf/subscribe [:db [state-key id]])
            items (vec items)
            base-order (range (count items))
            order (or temp-order base-order)]
        [:ul.draggable-list
         (for [[idx position] (zipmap order (range))]
           [:li.draggable-list__item
            (merge
             {:key idx
              :class (condp = idx
                       drag-index "draggable-list__item--active"
                       hover-index "draggable-list__item--hovered"
                       nil)
              :draggable true
              :on-drag-start #(rf/dispatch [:db [state-key id :drag-index] idx])
              :on-drag-over (utils.ui/with-handler
                             #(if on-drag-over
                                (on-drag-over id order position)
                                (rf/dispatch [::on-drag-over id order position])))
              :on-drag-end (fn []
                             (rf/dispatch [:db [state-key id] {}])
                             (when on-reorder
                               (on-reorder (map items order))))}
             (when props-fn (props-fn idx)))
            (get items idx)])]))))

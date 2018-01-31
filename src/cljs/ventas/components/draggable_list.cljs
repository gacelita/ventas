(ns ventas.components.draggable-list
  (:require
   [ventas.events :as events]
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
     (-> db
         (assoc-in [state-key id :temp-order] (put-before order position drag-index))))))

(defn main-view [_ _]
  (let [id (gensym)]
    (fn [{:keys [on-reorder]} items]
      (let [items (vec items)
            base-order (range (count items))
            {:keys [temp-order drag-index]} @(rf/subscribe [::events/db [state-key id]])
            order (or temp-order base-order)]
        [:ul.draggable-list
         (for [[idx position] (zipmap order (range))]
           [:li.draggable-list__item
            {:key idx
             :class (when (= idx drag-index)
                      "draggable-list__item--active")
             :draggable true
             :on-drag-start #(rf/dispatch [::events/db [state-key id :drag-index] idx])
             :on-drag-over (utils.ui/with-handler
                             #(rf/dispatch [::on-drag-over id order position]))
             :on-drag-end (fn []
                            (rf/dispatch [::events/db [state-key id] {}])
                            (when on-reorder
                              (on-reorder (map items order))))}
            (get items idx)])]))))
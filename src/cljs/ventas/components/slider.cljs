(ns ventas.components.slider
  (:require
   [cljs.core.async :refer [<! >! alts! chan timeout]]
   [re-frame.core :as rf]
   [ventas.events :as events])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

#_"
  This namespace expects a `state-path` argument, which is a vector that will be used
  for getting and setting data in the db.
  Users of this ns are expected to fill that state in this way:
  {:slides [{:width 0 :height 0} ...]
   :orientation #{:vertical :horizontal}
   :visible-slides 3"

(def transition-duration-ms 250)

(defn- get-dimension [v]
  (if (= v ::viewport)
    (-> js/window .-innerWidth)
    v))

(rf/reg-sub
 ::offset
 (fn [db [_ state-path]]
   (let [{:keys [current-index slides orientation]} (get-in db state-path)]
     (* -1 (reduce (fn [sum idx]
                     (let [slide (get slides idx)]
                       (+ sum (if (= orientation :vertical)
                                (get-dimension (:height slide))
                                (get-dimension (:width slide))))))
                   0
                   (range current-index))))))

(rf/reg-sub
 ::slides
 (fn [db [_ state-path]]
   (let [{:keys [render-index slides visible-slides]} (get-in db state-path)]
     (when (and render-index slides)
       (->> (cycle slides)
            (drop render-index)
            (take (if (<= visible-slides (count slides))
                    (+ 2 (count slides))
                    (count slides))))))))

(def update-stage (atom nil))

(defn- update-current-index [db state-path increment]
  (let [{:keys [visible-slides slides]} (get-in db state-path)]
    (when (<= visible-slides (count slides))
      (if (= :started @update-stage)
        db
        (do
          (reset! update-stage :started)
          (go (<! (timeout transition-duration-ms))
              (rf/dispatch [::events/db.update
                            state-path
                            (fn [state]
                              (-> state
                                  (update :render-index #(mod (+ % increment) (count (:slides state))))
                                  (update :current-index #(- % increment))))])
              (reset! update-stage :finished))
          (update-in db state-path (fn [state]
                                     (-> state
                                         (update :current-index #(+ % increment))))))))))

(rf/reg-event-db
 ::next
 (fn [db [_ state-path]]
   (or (update-current-index db state-path 1)
       db)))

(rf/reg-event-db
 ::previous
 (fn [db [_ state-path]]
   (or (update-current-index db state-path -1)
       db)))

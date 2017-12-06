(ns ventas.components.slider
  (:require
   [cljs.core.async :as async :refer [timeout <! >! chan]]
   [re-frame.core :as rf]
   [ventas.events :as events])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

#_"
  This namespace expects a `state-path` argument, which is a vector that will be used
  for getting and setting data in the db.
  Users of this ns are expected to fill that state in this way:
  {:slides [{:width 0 :height 0} ...]
   :orientation #{:vertical :horizontal}"

(def transition-duration-ms 250)

(defn- process-state [{:keys [current-index render-index slides orientation]}]
  {:orientation (or orientation :horizontal)
   :current-index (or current-index 1)
   :render-index (or render-index (inc (or current-index 1)))
   :slides (vec slides)})

(rf/reg-sub
 ::offset
 (fn [db [_ state-path]]
   (let [{:keys [current-index slides orientation]} (process-state (get-in db state-path))]
     (* -1 (reduce (fn [sum idx]
                     (let [slide (get slides idx)]
                       (+ sum (if (= orientation :vertical)
                                (:height slide)
                                (:width slide)))))
                   0
                   (range current-index))))))

(rf/reg-sub
 ::slides
 (fn [db [_ state-path]]
   (let [{:keys [render-index slides]} (process-state (get-in db state-path))]
     (->> (cycle slides)
          (drop render-index)
          (take (+ 2 (count slides)))))))

(defn- update-current-index [db state-path increment]
  (go (<! (timeout transition-duration-ms))
      (rf/dispatch [::events/db.update state-path (fn [state]
                                                    (-> state
                                                        (update :render-index #(mod (+ % increment) (count (:slides state))))
                                                        (update :current-index #(- % increment))))]))
  (let [{:keys [slides]} (get-in db state-path)]
    (update-in db state-path (fn [state]
                               (-> state
                                   (update :current-index #(+ % increment)))))))

(rf/reg-event-db
 ::next
 (fn [db [_ state-path]]
   (update-current-index db state-path 1)))

(rf/reg-event-db
 ::previous
 (fn [db [_ state-path]]
   (update-current-index db state-path -1)))
(ns ventas.components.slider
  (:require
   [re-frame.core :as rf]))

#_"
  This namespace expects a `state-path` argument, which is a vector that will be used
  for getting and setting data in the db.
  Users of this ns are expected to fill that state in this way:
  {:slides [{:width 0 :height 0} ...]
   :orientation #{:vertical :horizontal}"

(defn- process-state [{:keys [current-index slides orientation]}]
  {:orientation (or orientation :horizontal)
   :current-index (or current-index 0)
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

(rf/reg-event-db
 ::next
 (fn [db [_ state-path]]
   (let [{:keys [slides]} (get-in db state-path)]
     (update-in db (conj state-path :current-index) #(mod (inc %) (count slides))))))

(rf/reg-event-db
 ::previous
 (fn [db [_ state-path]]
   (let [{:keys [slides]} (get-in db state-path)]
     (update-in db (conj state-path :current-index) #(mod (dec %) (count slides))))))
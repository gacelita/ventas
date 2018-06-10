(ns ventas.kafka.registry
  "Holds the active consumers and producers"
  (:require
   [mount.core :refer [defstate]]))

(defstate registry
  :start (atom {})
  :stop (doseq [[_ itm] @registry]
          (future-cancel itm)))

(defn add! [id itm]
  (when-let [existing (get @registry id)]
    (future-cancel existing))
  (swap! registry assoc id itm)
  itm)

(defn remove! [id]
  (let [itm (get @registry id)]
    (future-cancel itm)
    (swap! registry dissoc id)
    nil))
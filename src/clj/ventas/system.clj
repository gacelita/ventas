(ns ventas.system
  "Utilities for managing the state of the system"
  (:require
   [mount.core :as mount]
   [clojure.core.async :as core.async]))

(def default-subsystem-mapping
  {:db '[ventas.database/conn]
   :indexer '[ventas.search/indexer]
   :server '[ventas.server/server]
   :config '[ventas.config/config-loader]
   :kafka '[ventas.kafka.registry/registry
            ventas.kafka.producer/producer
            ventas.stats.indexer/indexer]})

(defn get-states [subsystems & {:keys [mapping] :or {mapping default-subsystem-mapping}}]
  (->> subsystems
       (mapcat (fn [kw]
                 (let [states (get mapping kw)]
                   (when-not states
                     (throw (Exception. (str "State " kw " does not exist"))))
                   states)))
       (map #(ns-resolve *ns* %))))

(defn r
  "Restarts subsystems. Example:
    (r :figwheel :db)
   This would restart figwheel and the database connection.
   Refer to keyword->state for the available subsystems."
  [& subsystems]
  (let [states (get-states subsystems)]
    (if (empty? states)
      :done
      (let [{:keys [stopped]} (apply mount/stop states)
            {:keys [started]} (apply mount/start states)]
        (core.async/put! (ventas.events/pub :init) true)
        {:stopped stopped
         :started started}))))
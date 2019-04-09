(ns ventas.system
  "Utilities for managing the state of the system"
  (:require
   [mount.core :as mount]
   [ventas.events :as events]
   [clojure.core.async :as core.async]))

(def default-subsystem-mapping
  {:config '[ventas.config/config]
   :db '[ventas.database/conn]
   :storage '[ventas.storage/storage-backend]
   :es-client '[ventas.search/elasticsearch]
   :tx-processor '[ventas.database.tx-processor/tx-processor]
   :es-indexer '[ventas.search/indexer]
   :server '[ventas.server/server]})

(def default-states
  (->> default-subsystem-mapping
       (vals)
       (map (comp (partial ns-resolve *ns*) first))))

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
        (core.async/put! (events/pub :init) true)
        {:stopped stopped
         :started started}))))
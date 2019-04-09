(ns ventas.database.tx-processor
  (:require
   [mount.core :refer [defstate]]
   [clojure.tools.logging :as log]
   [ventas.utils :as utils]
   [ventas.database :as db]))

(def ^:private callbacks (atom {}))

(defn add-callback! [id cb]
  (swap! callbacks assoc id cb))

(defn remove-callback! [id]
  (swap! callbacks dissoc id))

(defn start-tx-report-queue-loop! []
  (future
   (loop []
     (when-not (Thread/interrupted)
       (utils/interruptible-try
        (when-let [report (.take (db/tx-report-queue))]
          (doseq [[id callback] @callbacks]
            (try
              (log/debug "Processing" id "callback")
              (callback report)
              (catch Throwable e
                (log/error (str "Error executing tx-processor callback with ID " id) e))))))
       (recur)))))

(defstate tx-processor
  :start
  (do
    (log/info "Starting tx-processor")
    (start-tx-report-queue-loop!))
  :stop
  (do
    (log/info "Stopping tx-processor")
    (future-cancel tx-processor)))
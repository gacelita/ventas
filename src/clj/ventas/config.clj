(ns ventas.config
  (:require [cprop.core :refer [load-config]]
            [mount.core :as mount :refer [defstate]]))

(defstate config
  :start
    (do
      (println "Starting config")
      (load-config))
  :stop
    (do (println "Stopping config")))
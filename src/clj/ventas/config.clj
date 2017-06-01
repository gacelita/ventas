(ns ventas.config
  (:require [cprop.core :refer [load-config]]
            [ventas.util :refer [print-info]]
            [mount.core :as mount :refer [defstate]]))

(defstate config
  :start
    (do
      (print-info "Starting config")
      (load-config))
  :stop
    (do (print-info "Stopping config")))
(ns ventas.config
  (:require [outpace.config :refer [defconfig]]
            [outpace.config.repl :refer [reload]]
            [mount.core :as mount :refer [defstate]]))

;;
;; This is a configuration-only namespace
;;

(defconfig ^:required database-url)
(defconfig ^:required cljs-port)
(defconfig ^:required debug)
(defconfig ^:required http-port)
(defconfig ^:required base-url)
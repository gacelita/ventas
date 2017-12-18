(ns ventas.core
  (:gen-class)
  (:require
   [clojure.core.async :refer [go >!]]
   [mount.core :as mount]
   [ventas.events :as events]
   [ventas.database]
   [ventas.server]))

(defn -main [& args]
  (mount/start)
  (go (>! (events/pub :init) true)))


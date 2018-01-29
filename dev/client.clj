(ns client
  "Client defstates"
  (:require
   [clojure.java.io :as io]
   [figwheel-sidecar.repl-api :as figwheel]
   [figwheel-sidecar.components.css-watcher :as figwheel.css-watcher]
   [ventas.utils :as utils]
   [ventas.config :as config]
   [clojure.java.shell]
   [me.raynes.conch.low-level :as sh]
   [mount.core :as mount :refer [defstate]]
   [clojure.tools.namespace.dependency :as namespace.dependency]
   [clojure.tools.namespace.find :as namespace.find]
   [taoensso.timbre :refer [info]]
   [clojure.tools.namespace.parse :as namespace.parse]))

;; Figwheel

(alter-var-root
 #'figwheel.css-watcher/handle-css-notification
 (fn [_]
   (fn ventas-css-handler [watcher files]
     (when-let [changed-css-files (not-empty (filter #(.endsWith % ".css") (map str files)))]
       (let [figwheel-server (:figwheel-server watcher)
             sendable-files (map #(figwheel.css-watcher/make-css-file %) changed-css-files)]
         (figwheel.css-watcher/send-css-files figwheel-server sendable-files))))))

(defn figwheel-start []
  (when (config/get :embed-figwheel?)
    (info "Starting Figwheel")
    (figwheel/start-figwheel!)))

(defn figwheel-stop []
  (when (config/get :embed-figwheel?)
    (info "Stopping Figwheel")
    (figwheel/stop-figwheel!)))

(defstate figwheel :start (figwheel-start) :stop (figwheel-stop))

;; Sass

(defstate sass
  :start
  (do
    (info "Starting SASS")
    (sh/proc "lein" "auto" "sassc" "once"))
  :stop
  (do
    (info "Stopping SASS")
    (sh/destroy sass)))

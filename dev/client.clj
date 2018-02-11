(ns client
  "Client defstates"
  (:require
   [clojure.java.shell]
   [figwheel-sidecar.components.css-watcher :as figwheel.css-watcher]
   [figwheel-sidecar.repl-api :as figwheel]
   [me.raynes.conch.low-level :as sh]
   [mount.core :refer [defstate]]
   [taoensso.timbre :refer [info]]
   [ventas.config :as config]
   [ventas.plugin :as plugin]
   [ventas.theme :as theme]))

;; Figwheel

(alter-var-root
 #'figwheel.css-watcher/handle-css-notification
 (fn [_]
   (fn ventas-css-handler [watcher files]
     (when-let [changed-css-files (not-empty (filter #(.endsWith % ".css") (map str files)))]
       (let [figwheel-server (:figwheel-server watcher)
             sendable-files (map #(figwheel.css-watcher/make-css-file %) changed-css-files)]
         (figwheel.css-watcher/send-css-files figwheel-server sendable-files))))))

(defn- dev-build []
  (let [build (->> (figwheel-sidecar.config/get-project-builds)
                   (filter #(= (:id %) "app"))
                   (first))
        theme (theme/current)]
    (-> build
        (assoc-in [:compiler :main]
                  (-> theme (plugin/plugin) (:cljs-ns)))
        (assoc-in [:compiler :output-to]
                  (str "resources/public/files/js/compiled/"
                       (name theme)
                       ".js")))))

(defn- figwheel-options []
  (-> (figwheel-sidecar.config/->lein-project-config-source)
      (figwheel-sidecar.config/->config-data)
      (get-in [:data :figwheel])))

(defn figwheel-start []
  (when (config/get :embed-figwheel?)
    (let [build (dev-build)]
      (info "Starting Figwheel, main ns:" (get-in build [:compiler :main]))
      (figwheel/start-figwheel!
       (merge (figwheel-options)
              {:builds [build]
               :builds-to-start ["app"]})))))

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
    (let [process (sh/proc "lein" "auto" "sassc" "once")]
      (future (sh/stream-to-out process :out))
      process))
  :stop
  (do
    (info "Stopping SASS")
    (sh/destroy sass)))

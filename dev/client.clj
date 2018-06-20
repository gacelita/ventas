(ns client
  "Client defstates"
  (:require
   [clojure.java.shell]
   [client.auto :as auto]
   [figwheel-sidecar.components.css-watcher :as figwheel.css-watcher]
   [figwheel-sidecar.repl-api :as figwheel]
   [figwheel-sidecar.config :as figwheel.config]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [ventas.config :as config]
   [ventas.plugin :as plugin]
   [ventas.theme :as theme]))

(def figwheel-theme (atom nil))

(alter-var-root
 #'figwheel.css-watcher/handle-css-notification
 (fn [_]
   (fn ventas-css-handler [watcher files]
     (when-let [changed-css-files (not-empty (filter #(.endsWith % ".css") (map str files)))]
       (let [figwheel-server (:figwheel-server watcher)
             sendable-files (map #(figwheel.css-watcher/make-css-file %) changed-css-files)]
         (figwheel.css-watcher/send-css-files figwheel-server sendable-files))))))

(defn- dev-build []
  (let [build (->> (figwheel.config/get-project-builds)
                   (filter #(= (:id %) "app"))
                   (first))
        theme (or @figwheel-theme (theme/current))]
    (-> build
        (assoc-in [:compiler :main]
                  (-> theme (plugin/find) (:cljs-ns)))
        (assoc-in [:compiler :output-to]
                  (str "resources/public/files/js/compiled/"
                       (name theme)
                       ".js")))))

(defn- get-project []
  (-> (figwheel.config/->lein-project-config-source)
      (figwheel.config/->config-data)
      :data
      (assoc :root (.getAbsolutePath (clojure.java.io/file ".")))))

(defn figwheel-start []
  (when (config/get :embed-figwheel?)
    (let [build (dev-build)]
      (timbre/info "Starting Figwheel, main ns:" (get-in build [:compiler :main]))
      (figwheel/start-figwheel!
       (merge (:figwheel (get-project))
              {:builds [build]
               :builds-to-start ["app"]})))))

(defn figwheel-stop []
  (when (config/get :embed-figwheel?)
    (timbre/info "Stopping Figwheel")
    (figwheel/stop-figwheel!)))

(defstate figwheel :start (figwheel-start) :stop (figwheel-stop))

(defstate sass
  :start
  (do
    (timbre/info "Starting SASS")
    (auto/auto (get-project) "sassc" "once"))
  :stop
  (do
    (timbre/info "Stopping SASS")
    (future-cancel sass)))

(ns client
  "Client defstates"
  (:require
   [clojure.java.io :as io]
   [figwheel-sidecar.repl-api :as figwheel]
   [figwheel-sidecar.components.css-watcher :as figwheel.css-watcher]
   [ventas.util :as util]
   [clojure.java.shell]
   [me.raynes.conch.low-level :as sh]
   [mount.core :as mount :refer [defstate]]
   [clojure.tools.namespace.dependency :as namespace.dependency]
   [clojure.tools.namespace.find :as namespace.find]
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
  (util/print-info "Starting Figwheel")
  (figwheel/start-figwheel!))

(defn figwheel-stop []
  (util/print-info "Stopping Figwheel")
  (figwheel/stop-figwheel!))

(defstate figwheel :start (figwheel-start) :stop (figwheel-stop))

;; Sass

(def sass-process)
(defn sass-start []
  (future
   (util/print-info "Starting SASS")
   (alter-var-root #'sass-process (sh/proc "lein" "auto" "sassc" "once"))))

(defn sass-stop []
  (future
   (util/print-info "Stopping SASS")
   (sh/destroy sass-process)))

(defstate sass :start (sass-start) :stop (sass-stop))

(defn detect-circular-dependencies! []
  (let [project-graph (atom (namespace.dependency/graph))]
    (->>
     (namespace.find/find-ns-decls-in-dir (io/file "src") namespace.find/cljs)
     (map (fn [decl]
            (let [name (namespace.parse/name-from-ns-decl decl)]
              (doseq [dep (namespace.parse/deps-from-ns-decl decl)]
                (swap! project-graph namespace.dependency/depend name dep)))))
     doall)
    true))
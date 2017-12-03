(ns repl
  "REPL-driven development"
  (:require
   [clojure.tools.namespace.repl :as tn]
   [mount.core :as mount]
   [clojure.core.async :refer [>! go]]
   [ventas.events :as events]
   [clojure.spec.alpha :as spec]))

(def aliases
  {'config 'ventas.config
   'server 'ventas.server
   'ws 'ventas.server.ws
   'api 'ventas.server.api
   'db 'ventas.database
   'schema 'ventas.database.schema
   'seed 'ventas.database.seed
   'entity 'ventas.database.entity
   'd 'datomic.api
   'utils 'ventas.utils
   'plugin 'ventas.plugin})

(defn init-aliases []
  (doseq [[from to] aliases]
    (ns-unalias 'repl from)
    (alias from to)))

(defmacro add-dependency
  "A macro for adding a dependency via Pomegranate.
   Usage: (add-dependency [incanter \"1.2.3\"])"
  [dependency]
  `(do (~'use '[cemerick.pomegranate :only (~'add-dependencies)])
       (~'add-dependencies :coordinates '[~dependency]
        :repositories (~'merge cemerick.pomegranate.aether/maven-central
                       {"clojars" "http://clojars.org/repo"}))))

;; Lifecycle
(defn init []
  (in-ns 'repl)
  (let [result (tn/refresh-all)]
    (when (instance? Exception result)
      (throw result))
    (mount/start)
    (init-aliases)
    (go (>! (events/pub :init) true))
    :done))

(defn reset []
  (mount/stop)
  (tn/refresh :after 'mount/start)
  (init-aliases)
  (go (>! (events/pub :init) true))
  :done)

(defn keyword->state [kw]
  (get {:figwheel 'client/figwheel
        :sass 'client/sass
        :db 'ventas.database/db
        :server 'ventas.server/server}
       kw))

(defn r
  "Reloads changed namespaces, and restarts the defstates within them.
   Accepts optional keywords representing defstates to restart (regardless of
   a need for it)
   Example:
     (r :figwheel :db)
   Refer to keyword->state to see what states can be restarted in this way"
  [& states]
  (let [states (->> states
                    (map keyword->state)
                    (map #(ns-resolve 'repl %)))
        _ (when (seq states)
            (apply mount/stop states))
        result (tn/refresh)]
    (when (or (instance? Exception result)
              (instance? Error result))
      (throw result))
    (when (seq states)
      (apply mount/start states))
    (when (= (ns-name *ns*) 'repl)
      (init-aliases))
    (go (>! (events/pub :init) true))
    :done))

(defn run-tests []
  (clojure.test/run-all-tests #"ventas.*?\-test"))

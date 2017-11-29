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

(defn r []
  (let [result (tn/refresh)]
    (if (instance? Exception result)
      (throw result)
      (when (= (str *ns*) "repl")
        (init-aliases)
        (go (>! (events/pub :init) true))
        :done))))

(defn run-tests []
  (clojure.test/run-all-tests #"ventas.*?\-test"))

(defmacro start-frontend []
  '(do (mount/start #'client/figwheel #'client/sass)))

(defmacro reset-frontend []
  '(do (mount/stop #'client/figwheel #'client/sass)
       (tn/refresh)
       (start-frontend)
       (init-aliases)
       (go (>! (events/pub :init) true))
       :done))

(defmacro start-backend []
  '(do (mount/start #'ventas.database/db #'ventas.server/server)))

(defmacro reset-backend []
  '(do (mount/stop #'ventas.database/db #'ventas.server/server)
       (tn/refresh)
       (start-backend)
       (init-aliases)
       (go (>! (events/pub :init) true))
       :done))
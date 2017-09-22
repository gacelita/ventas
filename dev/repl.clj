(ns repl
  "REPL-driven development"
  (:require
   [clojure.tools.namespace.repl :as tn]
   [mount.core :as mount]
   [clojure.core.async :refer [>! go]]
   [ventas.events :as events]
   [clojure.spec.alpha :as spec]))

(defmacro init-aliases
  "A macro for initializing any aliases that may be useful during development.
   Very different from (:requiring :as...) in this namespace's ns form, since that
   would make our REPL unbootable if the application does not compile"
  []
  `(do
     (~'ns-unalias ~''repl ~''config)
     (~'ns-unalias ~''repl ~''server)
     (~'ns-unalias ~''repl ~''api)
     (~'ns-unalias ~''repl ~''db)
     (~'ns-unalias ~''repl ~''schema)
     (~'ns-unalias ~''repl ~''seed)
     (~'ns-unalias ~''repl ~''entity)
     (~'ns-unalias ~''repl ~''d)
     (~'ns-unalias ~''repl ~''adi)
     (~'ns-unalias ~''repl ~''util)
     (~'alias ~''config 'ventas.config)
     (~'alias ~''server 'ventas.server)
     (~'alias ~''api 'ventas.server.api)
     (~'alias ~''db 'ventas.database)
     (~'alias ~''schema 'ventas.database.schema)
     (~'alias ~''seed 'ventas.database.seed)
     (~'alias ~''entity 'ventas.database.entity)
     (~'alias ~''d 'datomic.api)
     (~'alias ~''util 'ventas.util)))

(defmacro set-config
  "A macro for setting configuration values on runtime.
  Usage: (set-config cljs-port 3001)"
  [k v]
  `(do (~'ns ventas.config)
       (~'def ~k ~v)
       (~'ns ~'user)))

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
    (go (>! events/init true))))

(defn reset []
  (mount/stop)
  (tn/refresh :after 'mount/start)
  (init-aliases)
  (go (>! events/init true))
  :done)

(defn r []
  (let [result (tn/refresh)]
    (if (instance? Exception result)
      (throw result)
      (do
        (init-aliases)
        (go (>! events/init true))
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
       (go (>! events/init true))
       :done))

(defmacro start-backend []
  '(do (mount/start #'ventas.database/db #'ventas.server/server #'ventas.config/config)))

(defmacro reset-backend []
  '(do (mount/stop #'ventas.database/db #'ventas.server/server #'ventas.config/config)
       (tn/refresh)
       (start-backend)
       (init-aliases)
       (go (>! events/init true))
       :done))
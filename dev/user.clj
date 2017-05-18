(ns user
  (:require [figwheel-sidecar.repl-api :as figwheel]
            [mount.core :as mount :refer [defstate]]
            [clojure.java.shell]
            [me.raynes.conch.low-level :as sh]
            [clojure.tools.namespace.repl :as tn]
            [clojure.stacktrace :as st :refer [print-stack-trace]]
            [ventas.util :refer [print-info]]
            [clojure.repl :refer :all]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; Tips for the REPL:
;; - Never move out of this namespace
;; - Never use (use ...), because that wreaks havoc with tn/refresh (with the sole exception of pomegranate)
;; - Use (go) when you want to launch the app for the first time
;; - Use (reset) when you want to nuke the app and launch it again
;; - Do not require this ns from anywhere or face the consequences of cyclic doom
;; - Use (require '[ventas.something] :refer rgs) when you want to use something
;; - If you want to load a dependency, use the "add-dependency" macro below
;; - For changing the configuration (originally fetched from config.edn), use the "set-config" macro below
;; - You can load some useful aliases by calling "init-aliases"

(defmacro init-aliases
  "A macro for initializing any aliases that may be useful during development.
   Very different from (:requiring :as...) in this namespace's ns form, since that 
   would make our REPL unbootable if the application does not compile"
  []
  `(do
      (~'ns-unalias ~''user ~''config)
      (~'ns-unalias ~''user ~''server)
      (~'ns-unalias ~''user ~''db)
      (~'ns-unalias ~''user ~''schema)
      (~'ns-unalias ~''user ~''d)
      (~'ns-unalias ~''user ~''adi)
      (~'ns-unalias ~''user ~''util)
      (~'alias ~''config 'ventas.config)
      (~'alias ~''server 'ventas.server)
      (~'alias ~''db 'ventas.database)
      (~'alias ~''schema 'ventas.database-schema)
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

;; Include "dev" (not sure if still needed)
(clojure.tools.namespace.repl/set-refresh-dirs "src/clj" "src/cljc" "dev")

;; CLJS REPL
(def browser-repl figwheel/cljs-repl)


;; Figwheel

(defn figwheel-start []
  (print-info "Starting Figwheel")
  (figwheel/start-figwheel!))

(defn figwheel-stop []
  (print-info "Stopping Figwheel")
  (figwheel/stop-figwheel!))

(defstate figwheel :start (figwheel-start) :stop (figwheel-stop))


;; Sass

(def sass-process)
(defn sass-start []
  (future
    (print-info "Starting SASS")
    (alter-var-root #'sass-process (sh/proc "lein" "auto" "sassc" "once"))))

(defn sass-stop []
  (future
    (print-info "Stopping SASS")
    (sh/destroy sass-process)))

(defstate sass :start (sass-start) :stop (sass-stop))


;; Lifecycle

(def start mount/start)

(defn init []
  (tn/refresh-all)
  (mount/start)
  (init-aliases)
  :ready)

(defn reset []
  (mount/stop)
  (tn/refresh :after 'user/start)
  (init-aliases)
  :resetted)

(defn r []
  (tn/refresh)
  (init-aliases)
  :done)

(defmacro start-frontend []
  '(do (mount/start #'user/figwheel #'user/sass)))

(defmacro reset-frontend []
  '(do (mount/stop #'user/figwheel #'user/sass)
      (tn/refresh)
      (start-frontend)
      (init-aliases)
      :resetted)) 

(defmacro start-backend []
  '(do (mount/start #'ventas.database/db #'ventas.server/server #'ventas.config/config)))

(defmacro reset-backend []
  '(do (mount/stop #'ventas.database/db #'ventas.server/server #'ventas.config/config)
      (tn/refresh)
      (start-backend)
      (init-aliases)
      :resetted)) 
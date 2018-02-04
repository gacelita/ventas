(ns repl
  "REPL-driven development.
   Don't add ventas.* aliases in this ns, as it conflicts with dynamic aliases
   and nonstrict classloading."
  (:require
   [cemerick.pomegranate :as pomegranate]
   [cemerick.pomegranate.aether :as aether]
   [clojure.core.async :refer [>! go]]
   [clojure.spec.alpha :as spec]
   [clojure.tools.namespace.repl :as tn]
   [mount.core :as mount]
   [ventas.config]
   [ventas.events]
   [ventas.theme]))

(when (ventas.config/get :strict-classloading)
  ;; ensures all code is required - avoiding issues:
  (require 'ventas.core))

(def aliases
  {'api 'ventas.server.api
   'common.utils 'ventas.common.utils
   'config 'ventas.config
   'd 'datomic.api
   'db 'ventas.database
   'entity 'ventas.database.entity
   'plugin 'ventas.plugin
   'schema 'ventas.database.schema
   'search 'ventas.search
   'seed 'ventas.database.seed
   'server 'ventas.server
   'utils 'ventas.utils
   'ws 'ventas.server.ws})

(defn deinit-aliases []
  (doseq [[from to] aliases]
    (ns-unalias 'repl from)))

(defn init-aliases []
  (deinit-aliases)
  (doseq [[from to] aliases]
    (alias from to)))

(defn add-dependency [coordinates]
  (pomegranate/add-dependencies
   :coordinates [coordinates]
   :repositories (merge aether/maven-central
                        {"clojars" "https://clojars.org/repo"})))


(defmacro add-dependency
  "A macro for adding a dependency via Pomegranate.
   Usage: (add-dependency [incanter \"1.2.3\"])"
  [dependency]
  `(do (~'use '[cemerick.pomegranate :only (~'add-dependencies)])
       (~'add-dependencies :coordinates '[~dependency]
                           :repositories (~'merge cemerick.pomegranate.aether/maven-central
                                                  {"clojars" "https://clojars.org/repo"}))))

;; Lifecycle
(defn init []
  (in-ns 'repl)
  (deinit-aliases)
  (let [result (tn/refresh-all)]
    (when (instance? Throwable result)
      (throw result))
    (mount/start)
    (init-aliases)
    (go (>! (ventas.events/pub :init) true))
    :done))

(defn reset []
  (deinit-aliases)
  (mount/stop)
  (tn/refresh :after 'mount/start)
  (init-aliases)
  (go (>! (ventas.events/pub :init) true))
  :done)

(defn keyword->state [kw]
  (get {:figwheel 'client/figwheel
        :sass 'client/sass
        :db 'ventas.database/db
        :indexer 'ventas.search/indexer
        :server 'ventas.server/server
        :config 'ventas.config/config-loader}
       kw))

(defn r
  "Reloads changed namespaces, and restarts the defstates within them.
   Accepts optional keywords representing defstates to restart (regardless of
   a need for it)
   Example:
     (r :figwheel :db)
   Refer to keyword->state to see what states can be restarted in this way"
  [& states]
  (when (= (ns-name *ns*) 'repl)
    (deinit-aliases))
  (let [states (->> states
                    (map keyword->state)
                    (map #(ns-resolve 'repl %)))
        _ (when (seq states)
            (apply mount/stop states))
        result (tn/refresh)]
    (when (instance? Throwable result)
      (throw result))
    (when (seq states)
      (apply mount/start states))
    (when (= (ns-name *ns*) 'repl)
      (init-aliases))
    (go (>! (ventas.events/pub :init) true))
    :done))

(defn run-tests []
  (clojure.test/run-all-tests #"ventas.*?\-test"))

(defn set-theme! [theme]
  (ventas.theme/set! theme)
  (mount.core/stop (ns-resolve 'repl 'client/figwheel))
  (mount.core/start (ns-resolve 'repl 'client/figwheel)))
(ns repl
  "REPL-driven development.
   Don't add ventas.* aliases in this ns, as it conflicts with dynamic aliases
   and nonstrict classloading.
   Don't depend on anything that depends on other ventas namespaces, either."
  (:require
   [cemerick.pomegranate :as pomegranate]
   [cemerick.pomegranate.aether :as aether]
   [clojure.tools.namespace.repl :as tn]
   [mount.core :as mount]
   [ventas.config]
   [ventas.events]
   [client]
   ;; Aliases for REPL usage
   [clojure.core.async :as core.async :refer [<! chan >! go]]
   [clojure.spec.alpha :as spec]
   [taoensso.timbre :as timbre]))

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
   'site 'ventas.site
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

(defn keyword->states [kw]
  (get {:figwheel '[client/figwheel]
        :sass '[client/sass]
        :db '[ventas.database/conn]
        :indexer '[ventas.search/indexer]
        :server '[ventas.server/server]
        :config '[ventas.config/config-loader]
        :sites '[ventas.site/sites]
        :kafka '[ventas.kafka.registry/registry
                 ventas.kafka.producer/producer
                 ventas.stats.indexer/indexer]}
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
                    (mapcat (fn [kw]
                              (let [states (keyword->states kw)]
                                (when-not states
                                  (throw (Exception. (str "State " kw " does not exist"))))
                                states)))
                    (map (fn [state]
                           (ns-resolve 'repl state))))
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
  (reset! client/figwheel-theme theme)
  (mount.core/stop (ns-resolve 'repl 'client/figwheel))
  (mount.core/start (ns-resolve 'repl 'client/figwheel)))

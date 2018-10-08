(ns ventas.core
  (:refer-clojure :exclude [reset!])
  (:require
   [clojure.core.async :as core.async :refer [>! go]]
   [clojure.tools.nrepl.server :as nrepl]
   [mount.core :as mount]
   [taoensso.timbre :as timbre]
   [ventas.config :as config]
   [ventas.database :as db]
   [ventas.database.seed :as seed]
   [ventas.email.templates.core]
   [ventas.entities.core]
   [ventas.entities.image-size :as entities.image-size]
   [ventas.events :as events]
   [ventas.kafka.producer :as kafka.producer]
   [ventas.kafka.registry :as kafka.registry]
   [ventas.logging]
   [ventas.plugins.core]
   [ventas.search :as search]
   [ventas.search.indexing :as search.indexing]
   [ventas.seo :as seo]
   [ventas.server :as server]
   [ventas.server.api.admin]
   [ventas.server.api.description]
   [ventas.server.api.user]
   [ventas.server.api]
   [ventas.stats.indexer :as stats.indexer]
   [ventas.themes.blank.core]
   [ventas.themes.clothing.core])
  (:gen-class))

(defn start! []
  (mount/start #'config/config-loader
               #'db/conn
               #'search/elasticsearch
               #'search/indexer
               #'search.indexing/tx-report-queue-loop
               #'seo/driver
               #'server/server
               #'stats.indexer/indexer
               #'kafka.producer/producer
               #'kafka.registry/registry)
  (core.async/put! (events/pub :init) true))

(defn reset!
  "Returns everything to its default state, removing all data"
  []
  (seed/seed :recreate? true)
  (search.indexing/reindex)
  (entities.image-size/clean-storage)
  (entities.image-size/transform-all))

(defn -main [& args]
  (start!)
  (let [auth-secret (config/get :auth-secret)]
    (when (or (empty? auth-secret) (= auth-secret "CHANGEME"))
      (throw (Exception. (str ":auth-secret is empty or has not been changed.\n"
                              "Either edit resources/config.edn or add an AUTH_SECRET environment variable, and try again.")))))
  (let [{:keys [host port]} (config/get :nrepl)]
    (timbre/info (str "Starting nREPL server on " host ":" port))
    (nrepl/start-server :port port :bind host))
  (when (config/get :reset-on-restart)
    (timbre/info ":reset-on-restart is on -- removing all data")
    (reset!)))
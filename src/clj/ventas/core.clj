(ns ventas.core
  (:refer-clojure :exclude [reset!])
  (:require
   [clojure.tools.logging :as log]
   [clojure.core.async :as core.async :refer [>! go]]
   [mount.core :as mount]
   [ventas.config :as config]
   [ventas.database :as db]
   [ventas.database.schema :as schema]
   [ventas.database.seed :as seed]
   [ventas.database.tx-processor :as tx-processor]
   [ventas.email.templates.core]
   [ventas.entities.core]
   [ventas.events :as events]
   [ventas.i18n.cldr :as cldr]
   [ventas.search :as search]
   [ventas.search.indexing]
   [ventas.server :as server]
   [ventas.server.api.core]
   [ventas.entities.file :as entities.file])
  (:gen-class))

(defn start-system! [& [states]]
  (let [states (or states [#'config/config
                           #'db/conn
                           #'search/elasticsearch
                           #'search/indexer
                           #'tx-processor/tx-processor
                           #'server/server])]
    (apply mount/start states)
    (core.async/put! (events/pub :init) true)))

(defn -main [& args]
  (log/info "Starting up...")
  (start-system!))

(defn resource-as-stream [s]
  (let [loader (.getContextClassLoader (Thread/currentThread))]
    (.getResourceAsStream loader s)))

(defn setup!
  "- Migrates the db
   - Imports CLDR data
   - Transacts entity and plugin fixtures
   - Sets the ventas logo as the logo"
  [& {:keys [recreate?]}]
  (schema/migrate :recreate? recreate?)
  (seed/seed)
  (cldr/import-cldr!)
  (entities.file/create-from-file! (resource-as-stream "ventas/logo.png") "png" :logo)
  :done)
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
   [ventas.email.templates.core]
   [ventas.entities.core]
   [ventas.events :as events]
   [ventas.i18n.cldr :as cldr]
   [ventas.search :as search]
   [ventas.search.indexing :as search.indexing]
   [ventas.server :as server]
   [ventas.server.api.admin]
   [ventas.server.api.description]
   [ventas.server.api.user]
   [ventas.server.api]
   [ventas.themes.admin.core])
  (:gen-class))

(defn start-system! [& [states]]
  (let [states (or states [#'config/config
                           #'db/conn
                           #'search/elasticsearch
                           #'search/indexer
                           #'search.indexing/tx-report-queue-loop
                           #'server/server])]
    (apply mount/start states)
    (core.async/put! (events/pub :init) true)))

(defn -main [& args]
  (log/info "Starting up...")
  (start-system!))

(defn setup-db!
  "- Migrates the db
   - Imports CLDR data
   - Transacts entity and plugin fixtures"
  [& {:keys [recreate?]}]
  (schema/migrate :recreate? recreate?)
  (cldr/import-cldr!)
  (seed/seed))
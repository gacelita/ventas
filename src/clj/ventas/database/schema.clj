(ns ventas.database.schema
  (:require
   [io.rkn.conformity :as conformity]
   [ventas.config :refer [config]]
   [ventas.database :as db :refer [db]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [ventas.util :as util]
   [taoensso.timbre :as timbre :refer (trace debug info warn error)]))

(time-format/unparse (time-format/formatters :basic-date-time) (time/now))

(defn- get-migrations []
  "Returns a list of migrations"
  (let [files (sort (.listFiles (io/file "resources/migrations")))]
    (map (fn [file]
           {(keyword (.getName file)) {:txes [(read-string (slurp file))]}})
         files)))

(defn create-migration
  "Creates a migration.
   Usage:
     (generate-migration 'my-migration')
     (generate-migration 'my-migration' [{:db/ident :product-variation/product
                                          :db/valueType :db.type/ref
                                          :db/cardinality :db.cardinality/one}])"
  ([code] (create-migration code nil))
  ([code initial-contents]
   (let [now (time/now)
         date (time-format/unparse (time-format/formatter "yyyy_MM_dd") now)
         hours (read-string (time-format/unparse (time-format/formatter "h") now))
         minutes (read-string (time-format/unparse (time-format/formatter "m") now))
         seconds (read-string (time-format/unparse (time-format/formatter "s") now))
         dt-identifier (str date "_" (+ (* 60 60 hours) (* 60 minutes) seconds))]
     (spit (str "resources/migrations/" dt-identifier "_" code ".edn") initial-contents))))

(defn delete-migration [code]
  "Deletes a migration.
   Usage:
     (delete-migration 'my-migration')"
  (if-let [file (first (util/find-files "resources/migrations" (re-pattern (str ".*?" code ".*?"))))]
    (.delete file)))

(defn migrate
  "Migrates the database."
  ([] (migrate false))
  ([recreate]
   (let [database-url (get-in config [:database :url])
         migrations (get-migrations)]
     (when recreate
       (mount.core/stop #'db/db)
       (db/recreate)
       (mount.core/start #'db/db))
     (doseq [migration migrations]
       (info "Running migration" (first (keys migration)))
       (info (conformity/ensure-conforms db/db migration))))))
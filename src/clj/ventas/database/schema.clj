(ns ventas.database.schema
  (:require
   [io.rkn.conformity :as conformity]
   [ventas.config :as config]
   [ventas.database :as db :refer [db]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [ventas.util :as util]
   [taoensso.timbre :as timbre :refer (trace debug info warn error)]))

(defn- ^:deprecated get-migrations-from-files []
  "Returns a list of migrations"
  (let [files (sort (.listFiles (io/file "resources/migrations")))]
    (map (fn [file]
           {(keyword (.getName file)) {:txes [(read-string (slurp file))]}})
         files)))

(defn ^:deprecated create-migration-file
  "Creates a migration file.
   Usage:
     (create-migration-file
       `my-migration`
       [{:db/ident :product.variation/product
         :db/valueType :db.type/ref
         :db/cardinality :db.cardinality/one}])"
  [kw & [initial-contents]]
  {:pre [(keyword? kw)]}
  (let [now (time/now)
        date (time-format/unparse (time-format/formatter "yyyy_MM_dd") now)
        hours (read-string (time-format/unparse (time-format/formatter "h") now))
        minutes (read-string (time-format/unparse (time-format/formatter "m") now))
        seconds (read-string (time-format/unparse (time-format/formatter "s") now))
        dt-identifier (str date "_" (+ (* 60 60 hours) (* 60 minutes) seconds))]
    (spit (str "resources/migrations/" dt-identifier "_" (name kw) ".edn") initial-contents)))

(defn ^:deprecated delete-migration-file [kw]
  "Deletes a migration.
   Usage:
     (delete-migration 'my-migration')"
  (if-let [file (first (util/find-files "resources/migrations" (re-pattern (str ".*?" (name kw) ".*?"))))]
    (.delete file)))

(defonce migrations
 (let [initial-schema
       [{:db/ident :schema/deprecated
         :db/valueType :db.type/boolean
         :db/cardinality :db.cardinality/one}

        {:db/ident :schema/see-instead
         :db/valueType :db.type/keyword
         :db/cardinality :db.cardinality/one}

        {:db/ident :schema/type
         :db/valueType :db.type/ref
         :db/cardinality :db.cardinality/one}

        {:db/ident :ventas/pluginId
         :db/valueType :db.type/keyword
         :db/cardinality :db.cardinality/one}

        {:db/ident :ventas/pluginVersion
         :db/valueType :db.type/string
         :db/cardinality :db.cardinality/one}]]
   (atom [{(keyword (str "hash-" (hash initial-schema))) initial-schema}])))

(defn get-migration [kw]
  (util/find-first
   (fn [v] (contains? (set (keys v)) kw))
   @migrations))

(defn register-migration!
  [entities]
  {:pre [(coll? entities)]}
  (let [key (keyword (str "hash-" (hash entities)))]
    (when-not (get-migration key)
      (swap! migrations conj {key entities}))))

(defn get-migrations []
  @migrations)

(defn migrate
  "Migrates the database."
  [& {:keys [recreate?]}]
  (when recreate?
    (mount.core/stop #'db/db)
    (db/recreate)
    (mount.core/start #'db/db))
  (let [database-url (config/get [:database :url])
        migrations (get-migrations)]
    (info "Running migrations")
    (doseq [migration migrations]
      (doseq [[k v] migration]
        (info "Migration " k)
        (db/ensure-conforms k v)))))

(register-migration!
 [{:db/ident :schema/deprecated
   :db/valueType :db.type/boolean
   :db/cardinality :db.cardinality/one}

  {:db/ident :schema/see-instead
   :db/valueType :db.type/keyword
   :db/cardinality :db.cardinality/one}

  {:db/ident :schema/type
   :db/valueType :db.type/ref
   :db/cardinality :db.cardinality/one}

  {:db/ident :ventas/pluginId
   :db/valueType :db.type/keyword
   :db/cardinality :db.cardinality/one}

  {:db/ident :ventas/pluginVersion
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one}])
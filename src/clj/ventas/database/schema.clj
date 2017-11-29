(ns ventas.database.schema
  (:require
   [io.rkn.conformity :as conformity]
   [ventas.config :as config]
   [ventas.database :as db :refer [db]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [ventas.utils :as utils]
   [taoensso.timbre :as timbre :refer (trace debug info warn error)]))

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
  (utils/find-first
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
  (let [migrations (get-migrations)]
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
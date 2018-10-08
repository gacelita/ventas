(ns ventas.database.schema
  (:require
   [taoensso.timbre :as timbre]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]))

(defn- make-migration [attrs]
  {(keyword (str "hash-" (hash attrs))) attrs})

(defn- initial-migrations []
  [(make-migration [{:db/ident :ventas/refEntityType
                     :db/valueType :db.type/keyword
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :ventas/pluginId
                     :db/valueType :db.type/keyword
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :ventas/pluginVersion
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}])
   (make-migration [{:db/ident :schema/deprecated
                     :db/valueType :db.type/boolean
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :schema/see-instead
                     :db/valueType :db.type/keyword
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :schema/type
                     :db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/one
                     :ventas/refEntityType :enum}])

   (make-migration [{:db/ident :event/kind
                     :db/valueType :db.type/keyword
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :ventas/slug
                     :db/unique :db.unique/identity
                     :db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/one
                     :db/isComponent true
                     :ventas/refEntityType :i18n}

                    {:db/ident :ventas/site
                     :db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/one
                     :ventas/refEntityType :site}])])

(defonce ^:private migrations
  (atom (initial-migrations)))

(defn reset-migrations! []
  (reset! migrations (initial-migrations)))

(defn get-migration [kw]
  (common.utils/find-first
   #(= (set (keys %)) #{kw})
   @migrations))

(defn- migration-index [kw]
  (common.utils/find-index
   #(= (set (keys %)) #{kw})
   @migrations))

(defn remove-migration [kw]
  (swap! migrations (fn [migrs]
                      (remove #(= (set (keys %)) #{kw})
                              migrs))))

(defn register-migration!
  "Takes a list of attributes and an optional migration key.
   Migrations can be replaced if the same migration key is used, but
   note that migrations will only run once during the lifetime of a database
   (hence you'd need to use (seed/seed :recreate? true) or an equivalent).
   This is why doing so generates a warning."
  [attributes & [key]]
  {:pre [(coll? attributes)]}
  (let [key (or key (keyword (str "hash-" (hash attributes))))
        pair {key attributes}]
    (if-let [migration (get-migration key)]
      (do
        (when (not= migration pair)
          (timbre/warn "Replacing migration with key" key))
        (swap! migrations assoc (migration-index key) pair))
      (swap! migrations conj pair))))

(defn get-migrations []
  @migrations)

(defn migrate
  "Migrates the database."
  [& {:keys [recreate?]}]
  (when recreate?
    (mount.core/stop #'db/conn)
    (db/recreate)
    (mount.core/start #'db/conn))
  (let [migrations (get-migrations)]
    (timbre/info "Running migrations")
    (doseq [migration migrations]
      (doseq [[k v] migration]
        (timbre/info "Migration " k)
        (db/ensure-conforms k v)))))

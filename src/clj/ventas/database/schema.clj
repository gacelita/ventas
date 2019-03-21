(ns ventas.database.schema
  (:require
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [clojure.tools.logging :as log]))

(defn- initial-migrations []
  [[:ref-entity-type [{:db/ident :ventas/refEntityType
                       :db/valueType :db.type/keyword
                       :db/cardinality :db.cardinality/one}]]
   [:base [{:db/ident :schema/deprecated
            :db/valueType :db.type/boolean
            :db/cardinality :db.cardinality/one}

           {:db/ident :schema/see-instead
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one}

           {:db/ident :schema/type
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :ventas/refEntityType :enum}

           {:db/ident :event/kind
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
            :ventas/refEntityType :site}]]])

(defonce ^:private migrations
  (atom (initial-migrations)))

(defn reset-migrations! []
  (reset! migrations (initial-migrations)))

(defn get-migration [kw]
  (get (into {} @migrations) kw))

(defn- migration-index [kw]
  (common.utils/find-index
   #(= (first %) kw)
   @migrations))

(defn remove-migration! [kw]
  (swap! migrations assoc (migration-index kw) nil))

(defn register-migration!
  "Takes a migration key and a list of attributes.
   Migrations can be replaced if the same migration key is used, but
   note that migrations will only run once during the lifetime of a database
   (hence you'd need to use (seed/seed :recreate? true) or an equivalent).
   This is why doing so generates a warning."
  [key attributes]
  {:pre [(coll? attributes) (keyword? key) (namespace key)]}
  (let [pair [key attributes]]
    (if-let [migration (get-migration key)]
      (do
        (when (not= migration pair)
          (log/warn "Replacing migration with key" key))
        (swap! migrations assoc (migration-index key) pair))
      (swap! migrations conj pair))))

(defn register-migrations!
  "Same as calling register-migration! many times, but makes it clear
   that the migrations have a certain order"
  [migrations]
  (doseq [[key attributes] migrations]
    (register-migration! key attributes)))

(defn get-migrations []
  (remove (comp nil? second) @migrations))

(defn migrate-one! [key]
  (let [migration (get-migration key)]
    (when-not migration
      (throw (Exception. (str "Migration " key " not found"))))
    (db/ensure-conforms key migration)))

(defn migrate
  "Migrates the database."
  [& {:keys [recreate?]}]
  (when recreate?
    (mount.core/stop #'db/conn)
    (db/recreate)
    (mount.core/start #'db/conn))
  (let [migrations (get-migrations)]
    (log/info "Running migrations")
    (doseq [[key attributes] migrations]
      (log/info "Migration " key)
      (db/ensure-conforms key attributes))))

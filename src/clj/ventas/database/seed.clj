(ns ventas.database.seed
  (:require
   [clojure.set :as set]
   [slingshot.slingshot :refer [throw+]]
   [taoensso.timbre :as timbre]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.schema :as schema]
   [ventas.plugin :as plugin]
   [ventas.theme :as theme]))

(defn- create*
  "Wraps create* with the seed lifecycle functions"
  [attrs]
  (let [attrs (entity/filter-seed attrs)
        _ (entity/before-seed attrs)
        entity (entity/create* attrs)]
    (entity/after-seed entity)))

(defn seed-type
  "Seeds the database with n entities of a type"
  [type n]
  (doseq [fixture (entity/fixtures type)]
    (create* fixture))
  (doseq [attributes (entity/generate (entity/kw->type type) n)]
    (create* attributes)))

(defn seed-type-with-deps
  [type n]
  (let [deps (entity/dependencies type)]
    (doseq [dep deps]
      (seed-type-with-deps dep 1))
    (timbre/info "Seeding type:" type)
    (seed-type type n)))

(defn- get-sorted-types*
  [current remaining]
  (if (seq remaining)
    (let [new-types (->> remaining
                         (map (fn [type]
                                [type (entity/dependencies type)]))
                         (into {})
                         (filter (fn [[type dependencies]]
                                   (or (empty? dependencies) (set/subset? dependencies (set current)))))
                         (keys))]
      (get-sorted-types*
       (into current new-types)
       (set/difference remaining new-types)))
    current))

(defn- detect-circular-dependencies! [types]
  (doseq [type types]
    (let [dependencies (entity/dependencies type)]
      (when (contains? dependencies type)
        (throw+ {:type ::self-dependency
                 :entity-type type
                 :message "An entity type cannot depend on itself"}))
      (doseq [dependency dependencies]
        (when-not (entity/type-exists? dependency)
          (throw+ {:type ::unexisting-type
                   :message "An entity type cannot depend on an unexisting entity type"
                   :entity-type type
                   :dependency dependency}))))))

(defn get-sorted-types
  "Returns the types in dependency order"
  []
  (let [types (entity/types)]
    (detect-circular-dependencies! types)
    (get-sorted-types* [] types)))

(defn seed
  "Migrates the database and transacts the fixtures.
   Options:
     recreate? - removes the db and creates a new one
     generate? - seeds the database with randomly generated entities
     minimal?  - seeds only the entity fixtures, ignoring plugin fixtures"
  [& {:keys [recreate? generate? minimal?]}]

  (schema/migrate :recreate? recreate?)
  (timbre/info "Migrations done!")

  (doseq [type (get-sorted-types)]
    (timbre/info "Seeding type " type)
    (seed-type type (if generate? (entity/seed-number type) 0)))

  (when-not minimal?
    (doseq [[id _] (plugin/all)]
      (timbre/info "Installing plugin " id)
      (doseq [fixture (plugin/fixtures id)]
        (create* fixture)))))

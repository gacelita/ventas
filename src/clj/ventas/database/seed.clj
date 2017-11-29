(ns ventas.database.seed
  (:require
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.schema :as schema]
   [taoensso.timbre :as timbre :refer [info]]
   [clojure.test.check.generators :as gen]
   [clojure.spec.alpha :as spec]
   [clojure.set :as set]
   [ventas.plugin :as plugin]))

(defn generate-1
  "Generate one sample of a given entity type"
  [type]
  (let [db-type (db/kw->type type)]
    (-> (gen/generate (spec/gen db-type))
        (assoc :schema/type db-type))))

(defn generate-n
  "Generates n samples of given spec"
  [type n]
  (map generate-1 (repeat n type)))

(defn seed-type
  "Seeds the database with n entities of a type"
  [type n]
  (doseq [fixture (entity/fixtures type)]
    (entity/create* fixture))
  (doseq [attributes (generate-n (db/kw->type type) n)]
    (let [seed-entity (entity/filter-seed attributes)
          _ (entity/before-seed seed-entity)
          entity (entity/create* seed-entity)]
      (entity/after-seed entity))))

(defn seed-type-with-deps
  [type n]
  (info "Seeding type with deps:" type)
  (let [deps (entity/dependencies type)]
    (doseq [dep deps]
      (seed-type-with-deps dep 1))
    (info "-> " type)
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
       (vec (concat current new-types))
       (set/difference remaining new-types)))
    current))

(defn- detect-circular-dependencies! [types]
  (doseq [type types]
    (let [dependencies (entity/dependencies type)]
      (when (contains? dependencies type)
        (throw (Error. (str "The type " type " depends on itself"))))
      (doseq [dependency dependencies]
        (when-not (entity/type-exists? dependency)
          (throw (Error. (str "The type " type " is depending on the type " dependency ", which does not exist"))))))))

(defn get-sorted-types
  "Returns the types in dependency order"
  []
  (let [types (entity/types)]
    (detect-circular-dependencies! types)
    (get-sorted-types* [] types)))

(defn seed
  "Seeds the database with sample data"
  [& {:keys [recreate?]}]
  (when recreate?
    (schema/migrate :recreate? recreate?))
  (info "Migrations done!")
  (doseq [type (get-sorted-types)]
    (info "Seeding type " type)
    (seed-type type (entity/seed-number type)))
  (doseq [plugin-kw (plugin/all-plugins)]
    (info "Seeding plugin " plugin-kw)
    (doseq [fixture (plugin/fixtures plugin-kw)]
      (entity/create* fixture))))
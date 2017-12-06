(ns ventas.database.seed
  (:require
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.schema :as schema]
   [taoensso.timbre :as timbre :refer [info]]
   [clojure.test.check.generators :as gen]
   [clojure.spec.alpha :as spec]
   [clojure.set :as set]
   [ventas.plugin :as plugin]
   [ventas.theme :as theme]))

(defn- seed-attrs [attrs]
  (let [attrs (entity/filter-seed attrs)
        _ (entity/before-seed attrs)
        entity (entity/create* attrs)]
    (entity/after-seed entity)))

(defn seed-type
  "Seeds the database with n entities of a type"
  [type n]
  (doseq [fixture (entity/fixtures type)]
    (seed-attrs fixture))
  (doseq [attributes (entity/generate (db/kw->type type) n)]
    (seed-attrs attributes)))

(defn seed-type-with-deps
  [type n]
  (let [deps (entity/dependencies type)]
    (doseq [dep deps]
      (seed-type-with-deps dep 1))
    (info "Seeding type:" type)
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

  (doseq [theme-kw (theme/all)]
    (info "Seeding theme " theme-kw)
    (doseq [fixture (theme/fixtures theme-kw)]
      (entity/create* fixture)))

  (doseq [plugin-kw (plugin/all)]
    (info "Seeding plugin " plugin-kw)
    (doseq [fixture (plugin/fixtures plugin-kw)]
      (entity/create* fixture))))
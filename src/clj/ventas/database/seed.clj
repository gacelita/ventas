(ns ventas.database.seed
  (:require [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.database.schema :as schema]
            [taoensso.timbre :as timbre :refer (trace debug info warn error)]
            [clojure.test.check.generators :as gen]
            [clojure.spec.alpha :as spec]))

(defn generate-1
  "Generate one sample of a given spec"
  [spec]
  (gen/generate (spec/gen spec)))

(defn generate-n
  "Generates n samples of given spec"
  [spec n]
  (let [generator (spec/gen spec)]
    (map (fn [_] (gen/generate generator)) (range n))))

(defn seed-type
  "Seeds the database with n entities of a type"
  [type n]
  (info "Seeding " type)
  (doseq [attributes (generate-n (db/kw->type type) n)]
    (let [seed-entity (entity/filter-seed attributes)
          _ (entity/before-seed seed-entity)
          entity (entity/transact seed-entity)]
      (entity/after-seed entity))))

(defn seed
  "Seeds the database with sample data"
  ([]
   (seed false))
  ([reset?]
   (let [types (keys @entity/registered-types)]
     (when reset?
       (schema/migrate true))
     (doseq [type types]
       (doseq [fixture (entity/fixtures type)]
         (entity/create type fixture)))
     (seed-type :tax 10)
     (seed-type :file 10)
     (seed-type :brand 10)
     (seed-type :configuration 20)
     (seed-type :resource 5)
     (seed-type :attribute 10)
     (seed-type :attribute-value 10)
     (seed-type :category 10)
     (seed-type :product 10)
     (seed-type :product-variation 10)
     (seed-type :user 10)
     (seed-type :country 10)
     (seed-type :state 10)
     (seed-type :address 10)
     (seed-type :order 10))))
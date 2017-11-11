(ns ventas.database.seed
  (:require [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.database.schema :as schema]
            [taoensso.timbre :as timbre :refer (trace debug info warn error)]
            [clojure.test.check.generators :as gen]
            [clojure.spec.alpha :as spec]))

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
  (info "Seeding " type)
  (doseq [attributes (generate-n (db/kw->type type) n)]
    (let [seed-entity (entity/filter-seed attributes)
          _ (entity/before-seed seed-entity)
          entity (entity/transact seed-entity)]
      (entity/after-seed entity))))

(defn seed
  "Seeds the database with sample data"
  [& {:keys [recreate?]}]
  (when recreate?
    (schema/migrate :recreate? recreate?))
  (doseq [type [:tax :file :brand :configuration :resource :attribute
                :attribute-value :category :product :product-variation :user
                :country :state :address :order :image-size]]
    (doseq [fixture (entity/fixtures type)]
      (entity/transact fixture))
    (seed-type type 10)))
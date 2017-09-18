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
  (doseq [entity-data (generate-n (keyword "schema.type" (name type)) n)]
    (let [entity-data (entity/preseed type entity-data)
          entity (entity/create type entity-data)]
      (entity/postseed entity))))

(def entity-list [:tax :file :brand :configuration :resource :attribute
                  :attribute-value :category :product :product-variation])

(defn seed
  "Seeds the database with sample data"
  ([]
   (seed false))
  ([reset?]
   (when reset?
     (schema/migrate true))
   (doseq [kw entity-list]
     (doseq [fixture (entity/fixtures kw)]
       (entity/create kw fixture)))
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
   (seed-type :order 10)))
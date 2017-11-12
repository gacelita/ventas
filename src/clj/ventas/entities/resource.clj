(ns ventas.entities.resource
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as gen']
   [ventas.database :as db]
   [ventas.database.entity :as entity]))

(spec/def :resource/keyword keyword?)
(spec/def :resource/name string?)
(spec/def :resource/file
  (spec/with-gen
   ::entity/ref
   #(gen/elements (map :db/id (entity/query :file)))))

(spec/def :schema.type/resource
  (spec/keys :req [:resource/keyword
                :resource/file]
          :opt [:resource/name]))

(entity/register-type!
 :resource
 {:attributes
  [{:db/ident :resource/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}

   {:db/ident :resource/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :resource/file
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :fixtures
  (fn []
    [{:resource/keyword :logo
      :resource/name "Logo"
      :schema/type :schema.type/resource
      :resource/file (:db/id (first (entity/query :file)))}])

  :dependencies
  #{:file}})

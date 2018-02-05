(ns ventas.plugins.featured-categories.core
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.seed :as seed]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]))

(def plugin-kw :featured-categories)

(plugin/register!
 plugin-kw
 {:version "0.1"
  :name "Featured categories"
  :fixtures
  (fn []
    (->> (entity/query :category)
         (take 4)
         (map #(assoc % :category/featured true))
         (map #(dissoc % :db/id))))
  :migrations
  [[{:db/ident :category/featured
     :db/valueType :db.type/boolean
     :db/cardinality :db.cardinality/one}]]})

(spec/def :category/featured boolean?)

(api/register-endpoint!
 ::featured-categories.list
 (fn [{:keys [params] :as message} {:keys [session]}]
   (->> (entity/query :category {:featured true})
        (map #(entity/to-json % {:culture (api/get-culture session)})))))

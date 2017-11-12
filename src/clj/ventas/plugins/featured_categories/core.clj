(ns ventas.plugins.featured-categories.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [clojure.spec.alpha :as spec]
   [ventas.database.seed :as seed]
   [ventas.database :as db]))

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
         (map #(dissoc % :db/id))))})

(spec/def :category/featured boolean?)

(plugin/register-plugin-migration!
 plugin-kw
 [{:db/ident :category/featured
   :db/valueType :db.type/boolean
   :db/cardinality :db.cardinality/one}])

(api/register-endpoint!
  ::featured-categories.list
  (fn [{:keys [params] :as message} state]
    (map entity/to-json (entity/query :category {:featured true}))))

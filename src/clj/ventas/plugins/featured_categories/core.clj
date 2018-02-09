(ns ventas.plugins.featured-categories.core
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]))

(spec/def :category/featured boolean?)

(api/register-endpoint!
 ::featured-categories.list
 (fn [_ {:keys [session]}]
   (->> (entity/query :category {:featured true})
        (map #(entity/to-json % {:culture (api/get-culture session)})))))

(plugin/register!
 :featured-categories
 {:name "Featured categories"
  :endpoints [::featured-categories.list]
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
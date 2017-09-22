(ns ventas.plugins.featured-products.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [ventas.database.schema :as schema]))

(plugin/register!
 :posts
 {:version "0.1"
  :name "Posts"})

(plugin/db-attributes!
 :posts
 [{:db/ident :post/name
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one}])

(api/register-endpoint!
  :featured-products.view
  (fn [{:keys [params] :as message} state]
    (entity/find (:id params))))

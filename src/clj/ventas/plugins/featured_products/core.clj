(ns ventas.plugins.featured-products.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [clojure.spec.alpha :as spec]))

(def plugin-kw :featured-products)

(plugin/register!
 plugin-kw
 {:version "0.1"
  :name "Featured products"})

(spec/def :product/featured boolean?)

(plugin/register-plugin-migration!
 plugin-kw
 [{:db/ident :product/featured
   :db/valueType :db.type/boolean
   :db/cardinality :db.cardinality/one}])

(api/register-endpoint!
  ::featured-products.list
  (fn [{:keys [params] :as message} state]
    (map entity/to-json (entity/query :product {:featured true}))))

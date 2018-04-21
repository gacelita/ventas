(ns ventas.plugins.featured-products.core
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]))

(spec/def :product/featured boolean?)

(api/register-endpoint!
 ::featured-products.list
 (fn [_ {:keys [session]}]
   (->> (entity/query :product {:featured true})
        (map (partial api/serialize-with-session session)))))

(plugin/register!
 :featured-products
 {:name "Featured products"
  :endpoints [::featured-products.list]
  :fixtures
  (fn []
    (->> (db/nice-query {:find ['?id]
                         :where '[[?id :product/images _]]})
         (map (comp entity/find :id))
         (map #(assoc % :product/featured true))
         (map #(dissoc % :db/id))))
  :migrations
  [[{:db/ident :product/featured
     :db/valueType :db.type/boolean
     :db/cardinality :db.cardinality/one}]]})

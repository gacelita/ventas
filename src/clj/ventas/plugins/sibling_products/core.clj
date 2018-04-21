(ns ventas.plugins.sibling-products.core
  (:require
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]))

(api/register-endpoint!
 ::list
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [id (api/resolve-ref id :product/keyword)
         {:product/keys [categories]} (entity/find id)]
     (->> (db/nice-query {:find '[?id]
                          :in {'?categories (set categories)
                               '?source-id id}
                          :where '[[?id :product/categories ?category]
                                   (not [(= ?id ?source-id)])
                                   [(contains? ?categories ?category)]]})
          (map (comp entity/find :id))
          (map (partial api/serialize-with-session session))))))

(plugin/register!
 :sibling-products
 {:name "Sibling products"
  :endpoints [::list]})

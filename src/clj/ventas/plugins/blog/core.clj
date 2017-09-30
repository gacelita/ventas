(ns ventas.plugins.blog.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]))

(plugin/register!
 :blog
 {:version "0.1"
  :name "Blog"})

(plugin/register-plugin-migration!
 :blog
 [{:db/ident :blog.post/name
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one}])

(entity/register-type! :blog.post)

(api/register-endpoint!
 :blog.list
 (fn [{:keys [params] :as message} state]
   (map entity/to-json
        (entity/query :blog))))

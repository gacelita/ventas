(ns ventas.plugins.blog.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [clojure.spec.alpha :as spec]))

(plugin/register!
 :blog
 {:version "0.1"
  :name "Blog"})

(spec/def :blog.post/name string?)

(spec/def :schema.type/blog.post
  (spec/keys :req [:blog.post/name]))

(entity/register-type!
 :blog.post
 {:attributes
  [{:db/ident :blog.post/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]})

(api/register-endpoint!
 :blog.post.create
 (fn [{:keys [params] :as message} state]
   (entity/upsert :blog.post params)))

(api/register-endpoint!
 :blog.list
 (fn [{:keys [params] :as message} {:keys [session]}]
   (map #(entity/to-json % {:culture (api/get-culture session)})
        (entity/query :blog.post))))

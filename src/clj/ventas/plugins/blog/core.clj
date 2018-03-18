(ns ventas.plugins.blog.core
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]))

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
 (fn [{:keys [params]} _]
   (entity/upsert :blog.post params)))

(api/register-endpoint!
 :blog.list
 (fn [_ {:keys [session]}]
   (map (partial api/serialize-with-session session)
        (entity/query :blog.post))))

(plugin/register!
 :blog
 {:name "Blog"
  :entity-types [:blog.post]
  :endpoints [:blog.post.create
              :blog.list]})
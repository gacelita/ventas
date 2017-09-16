;; Supongamos la creación de un plugin para "posts".
;; Tiene que crear la entidad "post" en la base de datos.
;; Tiene que incluir componentes para mostrar dichos posts.
;; Tiene que incluir endpoints para crear los posts.

;; Estructura:
;; ventas
;;   plugins
;;     posts
;;       clj
;;         core.clj
;;       cljs
;;         core.cljs

;; core.clj:

(ns ventas.plugins.posts.core)

(def plugin
  (map->Plugin {:version 0.1
                :name "Posts"
                :id :posts}))

(defdbtype :post
 [{:db/ident :post/name
   :db/valueType :db.type/string
   :db/cardinality :db.cardinality/one}])

(comment
 (c/ensure-conforms db/db [{:db/ident :post/name
                            :db/valueType :db.type/string
                            :db/cardinality :db.cardinality/one
                            :ventas/pluginId :posts
                            :ventas/pluginVersion 0.1}
                           {:db/ident :schema.type/plugins.posts.post}]))

(defendpoint ::view [{:keys [params] :as message} state]
  (ventas.database.entity/find (:id params))

;; core.cljs:

(defn post-view [post]
  [:div [:p (:name post)]])

(ventas.routes/define-routes!
 [{:route :frontend.post
   :name "Post view"
   :url ["post/" :id]}])
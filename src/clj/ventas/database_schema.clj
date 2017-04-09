(ns ventas.database-schema
  (:require
    [datomic.api :as d]
    [io.rkn.conformity :as c]
    [ventas.config :refer [database-url]]
    [ventas.database :only [db]]))


(def migration-basic
  "Basic fields"
  {:ventas/migration-basic
   {:txes [(list 
                {:db/ident :user/name
                 :db/valueType :db.type/string
                 :db/fulltext true
                 :db/index true
                 :db/cardinality :db.cardinality/one}

                {:db/ident :user/password
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one}

                {:db/ident :user/email
                 :db/valueType :db.type/string
                 :db/index true
                 :db/unique :db.unique/identity
                 :db/cardinality :db.cardinality/one}

                {:db/ident :user/friends
                 :db/valueType :db.type/ref,
                 :db/cardinality :db.cardinality/many}

                ;; Images

                {:db/ident :user/avatar
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/one}

                {:db/ident :image/extension
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/one}

                {:db/ident :image.extension/png}
                {:db/ident :image.extension/jpg}
                {:db/ident :image.extension/gif}
                {:db/ident :image.extension/tiff}

                ;; Status

                {:db/ident :user/status
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/one}

                {:db/ident :user.status/pending}
                {:db/ident :user.status/active}
                {:db/ident :user.status/inactive}
                {:db/ident :user.status/cancelled}

                ;; Comments

                {:db/ident :user/comments
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/many
                 :db/isComponent true}

                {:db/ident :comment/content
                 :db/valueType :db.type/string
                 :db/fulltext true
                 :db/cardinality :db.cardinality/one}

                {:db/ident :comment/target
                 :db/valueType :db.type/ref
                 :db/cardinality :db.cardinality/one}
                
              )]}})

(def migration-test
  "Test migration"
  {:ventas/migration-test
    {:txes [(list {:db/ident :test/test1 
               :db/valueType :db.type/string
               :db/cardinality :db.cardinality/one})]}})

(def migration-user-description
  ":user/description"
  {:ventas/migration-user-description
    {:txes [(list {:db/ident :user/description 
               :db/valueType :db.type/string
               :db/cardinality :db.cardinality/one})]}})

(def migration-deprecated
  ":schema/deprecated, :schema/see-instead"
  {:ventas/migration-deprecated
    {:txes [(list {:db/ident :schema/deprecated
                   :db/valueType :db.type/boolean
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :schema/see-instead
                   :db/valueType :db.type/keyword
                   :db/cardinality :db.cardinality/one})]}})

(def migration-comment-source
  ":comment/source"
  {:ventas/migration-comment-source
    {:txes [(list {:db/ident :comment/source
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :user/comments
                   :schema/deprecated true
                   :schema/see-instead :comment/source})]}})

(def migration-type
  ":schema/type"
  {:ventas/migration-type
    {:txes [(list {:db/ident :schema/type
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :schema.type/user}
                  {:db/ident :schema.type/comment}
                  {:db/ident :schema.type/image})]}})

(def migration-friendships
  ":friendship"
  {:ventas/migration-friendships
    {:txes [(list {:db/ident :friendship/source
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :friendship/target
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :user/friends
                   :schema/deprecated true
                   :schema/see-instead :friendship/source}

                  {:db/ident :schema.type/friendship})]}})

(def migration-tags
  ":image.tag"
  {:ventas/migration-tags
    {:txes [(list {:db/ident :image/source
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :image.tag/image
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :image.tag/target
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :image.tag/x
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :image.tag/y
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :image.tag/caption
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :schema.type/image.tag})]}})

(def migration-roles
  ":user/roles"
  {:ventas/migration-roles
    {:txes [(list {:db/ident :user/roles
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many}

                  {:db/ident :user.role/administrator}
                  {:db/ident :user.role/user})]}})

(def migrations [migration-basic
                 migration-test
                 migration-user-description
                 migration-deprecated
                 migration-comment-source
                 migration-type
                 migration-friendships
                 migration-tags
                 migration-roles])

(defn migrate 
  ([] (migrate false))
  ([recreate]
    (when recreate
      (println "Deleting database " database-url)
      (d/delete-database database-url)
      (println "Creating database " database-url)
      (d/create-database database-url))
    (let [connection (d/connect database-url)]
      (doseq [migration migrations]
        (println "Running migration" (first (keys migration)))
        (println (c/ensure-conforms connection migration))))))
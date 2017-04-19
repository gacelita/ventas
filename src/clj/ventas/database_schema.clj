(ns ventas.database-schema
  (:require
    [datomic.api :as d]
    [io.rkn.conformity :as c]
    [ventas.config :refer [config]]
    [ventas.database :only [db]]))

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
    (let [database-url (get-in config [:database :url])]
      (when recreate
        (println "Deleting database " database-url)
        (d/delete-database database-url)
        (println "Creating database " database-url)
        (d/create-database database-url))
      (let [connection (d/connect database-url)]
        (doseq [migration migrations]
          (println "Running migration" (first (keys migration)))
          (println (c/ensure-conforms connection migration)))))))
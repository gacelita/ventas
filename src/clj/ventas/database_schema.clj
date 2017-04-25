(ns ventas.database-schema
  (:require
    [datomic.api :as d]
    [io.rkn.conformity :as c]
    [ventas.config :refer [config]]
    [ventas.database :only [db]]
    [clojure.java.io :as io]
    [taoensso.timbre :as timbre :refer (trace debug info warn error)]))

(def migration-roles
  ":user/roles"
  {:ventas/migration-roles
    {:txes [(list {:db/ident :user/roles
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many}

                  {:db/ident :user.role/administrator}
                  {:db/ident :user.role/user})]}})

(defn get-migrations []
  (let [files (seq (.listFiles (io/file "resources/migrations")))]
    (map (fn [file] {(keyword (.getName file)) {:txes [(read-string (slurp file))]}}) files)))

(defn migrate 
  ([] (migrate false))
  ([recreate]
    (let [database-url (get-in config [:database :url])
          migrations (get-migrations)]
      (when recreate
        (info "Deleting database " database-url)
        (d/delete-database database-url)
        (info "Creating database " database-url)
        (d/create-database database-url))
      (let [connection (d/connect database-url)]
        (doseq [migration migrations]
          (info "Running migration" (first (keys migration)))
          (info (c/ensure-conforms connection migration)))))))
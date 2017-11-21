(ns ventas.test-tools
  (:require
   [datomic.api :as d]
   [ventas.database :as db]
   [ventas.database.schema :as schema]))

(defn create-test-uri []
  (str "datomic:mem://" (gensym "test")))

(defn test-conn []
  (let [uri (create-test-uri)]
    (d/create-database uri)
    (let [c (d/connect uri)]
      (with-redefs [db/db c]
        (schema/migrate))
      c)))

(ns ventas.test-tools
  (:require
   [datomic.api :as d]
   [ventas.database :as db]
   [clojure.java.io :as io]
   [ventas.database.seed :as seed]))

(defn create-test-uri [& [id]]
  (str "datomic:mem://" (or id (gensym "test"))))

(defn test-conn [& [id]]
  (let [uri (create-test-uri id)]
    (d/create-database uri)
    (let [c (d/connect uri)]
      (with-redefs [db/conn c]
        (seed/seed))
      c)))

(defmacro with-test-context [& body]
  `(with-redefs [db/conn (test-conn)]
     ~@body))

(defn with-test-image [f]
  (with-open [is (io/input-stream (io/resource "ventas/logo.png"))]
    (let [temp-file (java.io.File/createTempFile "ventas-logo" ".png")]
      (io/copy is temp-file)
      (f temp-file))))
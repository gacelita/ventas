(ns ventas.test-tools
  (:require
   [datomic.api :as d]
   [ventas.database :as db]
   [clojure.java.io :as io]
   [ventas.database.seed :as seed]
   [ventas.entities.core]
   [ventas.storage :as storage]
   [ventas.config :as config]
   [mount.core :as mount]))

(defn create-test-uri [& [id]]
  (str "datomic:mem://" (or id (gensym "test"))))

(defn test-conn [& [id]]
  (let [uri (create-test-uri id)]
    (d/create-database uri)
    (let [c (d/connect uri)]
      (with-redefs [db/conn c]
        (seed/seed))
      c)))

(defn test-storage-backend []
  (let [tmp-dir (System/getProperty "java.io.tmpdir")
        base-path (str tmp-dir "/" "ventas-test-storage")]
    (.mkdir (io/file base-path))
    (storage/->LocalStorageBackend base-path)))

(defmacro with-test-context [& body]
  `(with-redefs [db/conn (test-conn)
                 storage/storage-backend (test-storage-backend)]
     (mount/start #'config/config)
     ~@body))

(defn with-test-image [f]
  (with-open [is (io/input-stream (io/resource "ventas/logo.png"))]
    (let [temp-file (java.io.File/createTempFile "ventas-logo" ".png")]
      (io/copy is temp-file)
      (f temp-file))))
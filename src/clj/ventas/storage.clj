(ns ventas.storage
  (:require
   [clojure.java.io :as io]
   [mount.core :as mount :refer [defstate]]
   [ventas.storage.protocol :as protocol]))

(def
  ^{:dynamic true
    :doc "Should contain a StorageBackend implementation"}
  storage-backend)

(defrecord LocalStorageBackend []
  protocol/StorageBackend
  (get-object [_ key]
    (slurp (str "storage/" key)))
  (get-public-url [_ key]
    (str "/storage/" key))
  (stat-object [_ key]
    (let [file (io/file (str "storage/" key))]
      {:length (.length file)
       :last-modified (-> (.lastModified file)
                          (/ 1000) (long) (* 1000)
                          (java.util.Date.))}))
  (put-object [_ key file]
    (let [target-file (io/file (str "storage/" key))]
      (io/make-parents target-file)
      (when-not (.exists target-file)
        (io/copy file target-file)))))

(defstate storage-backend
  :start
  (do
    (.mkdir (io/file "storage"))
    (->LocalStorageBackend)))

(defn get-object [key]
  (protocol/get-object storage-backend key))

(defn get-public-url [key]
  (protocol/get-public-url storage-backend key))

(defn stat-object [key]
  (protocol/stat-object storage-backend key))

(defn put-object [key file]
  (protocol/put-object storage-backend key file))
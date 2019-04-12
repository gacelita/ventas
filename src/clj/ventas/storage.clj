(ns ventas.storage
  (:require
   [clojure.java.io :as io]
   [mount.core :as mount :refer [defstate]]
   [ventas.storage.protocol :as protocol]
   [clojure.string :as str])
  (:import [java.io File]))

(def
  ^{:dynamic true
    :doc "Should contain a StorageBackend implementation"}
  storage-backend)

(defn- key->path [key]
  (str "storage/" key))

(defn- key->file [key]
  (io/file (key->path key)))

(defrecord LocalStorageBackend []
  protocol/StorageBackend
  (get-object [_ key]
    (slurp (key->path key)))
  (get-public-url [_ key]
    (str "/" (key->path key)))
  (list-objects [this]
    (protocol/list-objects this ""))
  (list-objects [_ prefix]
    (->> (file-seq (io/file (str "storage" (when prefix (str "/" prefix)))))
         (map (fn [file]
                (-> (str file)
                    (str/replace (str "src" File/separator) "")
                    (str/replace File/separator "/"))))))
  (remove-object [_ key]
   (io/delete-file (key->file key) true))
  (stat-object [_ key]
    (let [file (key->file key)]
      {:length (.length file)
       :last-modified (-> (.lastModified file)
                          (/ 1000) (long) (* 1000)
                          (java.util.Date.))}))
  (put-object [_ key file]
    (let [target-file (key->file key)]
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

(defn remove-object [key]
  (protocol/remove-object storage-backend key))

(defn get-public-url [key]
  (protocol/get-public-url storage-backend key))

(defn stat-object [key]
  (protocol/stat-object storage-backend key))

(defn put-object [key file]
  (protocol/put-object storage-backend key file))

(defn list-objects [& [prefix]]
  (if prefix
    (protocol/list-objects storage-backend prefix)
    (protocol/list-objects storage-backend)))
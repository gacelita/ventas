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

(defn- key->path [base-path key]
  (str base-path (when key (str "/" key))))

(defn- key->file [base-path key]
  (io/file (key->path base-path key)))

(defrecord LocalStorageBackend [base-path]
  protocol/StorageBackend
  (get-object [_ key]
    (let [file (key->file base-path key)]
      (when (.exists file)
        (io/input-stream file))))
  (get-public-url [_ key]
    (str "/" (key->path base-path key)))
  (list-objects [this]
    (protocol/list-objects this ""))
  (list-objects [_ prefix]
    (->> (file-seq (io/file (key->path base-path prefix)))
         (map (fn [file]
                (-> (str file)
                    (str/replace (str "src" File/separator) "")
                    (str/replace File/separator "/"))))))
  (remove-object [_ key]
   (io/delete-file (key->file base-path key) true))
  (stat-object [_ key]
    (let [file (key->file base-path key)]
      (when (.exists file)
        {:length (.length file)
         :last-modified (-> (.lastModified file)
                            (/ 1000) (long) (* 1000)
                            (java.util.Date.))})))
  (put-object [_ key file]
    (let [target-file (key->file base-path key)
          file (if (string? file) (io/file file) file)]
      (io/make-parents target-file)
      (when-not (.exists target-file)
        (io/copy file target-file)))))

(defstate storage-backend
  :start
  (let [base-path "storage"]
    (.mkdir (io/file base-path))
    (->LocalStorageBackend base-path)))

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
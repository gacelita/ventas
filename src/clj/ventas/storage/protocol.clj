(ns ventas.storage.protocol)

(defprotocol StorageBackend
  (get-object [this key])
  (get-public-url [this key])
  (stat-object [this key])
  (put-object [this key file]))
(ns ventas.storage.protocol)

(defprotocol StorageBackend
  (get-object [this key])
  (get-public-url [this key])
  (stat-object [this key])
  (list-objects [this])
  (put-object [this key file]))
(ns ventas.storage.protocol)

(defprotocol StorageBackend
  (get-object [this key])
  (get-public-url [this key])
  (stat-object [this key])
  (remove-object [this key])
  (list-objects [this] [this prefix])
  (put-object [this key file]))
(ns ventas.config
  (:refer-clojure :exclude [get set])
  (:require
   [clojure.core :as clj]
   [cprop.core :refer [load-config]]
   [mount.core :as mount :refer [defstate]]))

(defonce ^:private config (atom (load-config)))

(defn set
  [k v]
  {:pre [(keyword? k)]}
  (swap! config assoc k v))

(defn get [k-or-ks]
  (if (coll? k-or-ks)
    (get-in @config k-or-ks)
    (clj/get @config k-or-ks)))

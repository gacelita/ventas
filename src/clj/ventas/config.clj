(ns ventas.config
  (:refer-clojure :exclude [get set])
  (:require
   [clojure.core :as clj]
   [cprop.core :as cprop]
   [cprop.source]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]))

(defn- load-config []
  (cprop/load-config :resource "default-config.edn"
                     :merge [(cprop.source/from-resource "config.edn")]))

(defonce ^:private config (atom (load-config)))

(defstate config-loader
  :start
  (let [config-data (load-config)]
    (timbre/info "Loading configuration")
    (timbre/debug config-data)
    (reset! config config-data)))

(defn set
  [k-or-ks v]
  {:pre [(or (keyword? k-or-ks) (sequential? k-or-ks))]}
  (let [ks (if (keyword? k-or-ks)
             [k-or-ks]
             k-or-ks)]
    (swap! config assoc-in ks v)))

(defn get [& ks]
  (get-in @config ks))

(ns ventas.config
  (:refer-clojure :exclude [get set])
  (:require
   [clojure.core :as clj]
   [cprop.core :as cprop]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]))

(def ^:private defaults
  {:server {:port 3450
            :host "localhost"}
   :elasticsearch {:index "ventas"
                   :port 9200
                   :host "127.0.0.1"}
   :debug false
   :embed-figwheel? true
   :cljs-port 3001
   :strict-classloading false})

(defn- load-config []
  (merge defaults (cprop/load-config)))

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

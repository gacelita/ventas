(ns ventas.config
  (:refer-clojure :exclude [get set])
  (:require
   [clojure.core :as clj]
   [cprop.core :refer [load-config]]))

(def ^:private defaults
  {:server {:port 3450
            :host "localhost"}
   :elasticsearch {:index "ventas"
                   :port 9200
                   :host "127.0.0.1"}
   :debug false
   :embed-figwheel? true
   :cljs-port 3001})

(defonce ^:private config (atom (merge defaults (load-config))))

(defn set
  [k v]
  {:pre [(keyword? k)]}
  (swap! config assoc k v))

(defn get [& ks]
  (get-in @config ks))

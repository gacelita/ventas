(ns ventas.config
  (:refer-clojure :exclude [get set])
  (:require
   [cprop.core :as cprop]
   [cprop.source]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [ventas.utils :as utils]))

(defn- load-config []
  (let [custom-config (utils/swallow
                        (cprop.source/from-resource "config.edn"))]
    (apply cprop/load-config
           :resource "default-config.edn"
           (when custom-config
             [:merge [custom-config]]))))

(defonce ^:dynamic config (atom (load-config)))

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
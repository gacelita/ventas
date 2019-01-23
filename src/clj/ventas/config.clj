(ns ventas.config
  (:refer-clojure :exclude [get set])
  (:require
   [cprop.core :as cprop]
   [cprop.source]
   [mount.core :refer [defstate]]
   [ventas.utils :as utils]
   [clojure.tools.logging :as log]))

(defn- load-config []
  (let [custom-config (utils/swallow
                        (cprop.source/from-resource "config.edn"))]
    (apply cprop/load-config
           :resource "ventas/config/base-config.edn"
           (when custom-config
             [:merge [custom-config]]))))

(defstate config
  :start
  (do
    (log/info "Loading configuration")
    (let [{:keys [auth-secret profile] :as config-data} (load-config)]
      (log/debug config-data)
      (when (and (= profile "prod") (or (empty? auth-secret) (= auth-secret "CHANGEME")))
        (throw (Exception. (str ":auth-secret is empty or has not been changed.\n"
                                "Either edit resources/config.edn or add an AUTH_SECRET environment variable, and try again."))))
      (atom config-data))))

(defn set
  [k-or-ks v]
  {:pre [(or (keyword? k-or-ks) (sequential? k-or-ks))]}
  (let [ks (if (keyword? k-or-ks)
             [k-or-ks]
             k-or-ks)]
    (swap! config assoc-in ks v)))

(defn get [& ks]
  (get-in @config ks))

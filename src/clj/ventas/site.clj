(ns ventas.site
  (:require
   [mount.core :refer [defstate]]
   [ventas.paths :as paths]
   [ventas.database :as db]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [ventas.common.utils :as common.utils]
   [taoensso.timbre :as timbre]))

(def ^:dynamic current)

(defn get-sites []
  (->> (paths/resolve ::paths/sites)
       (io/file)
       (file-seq)
       (remove #(.isDirectory %))
       (map (fn [file]
              [(str/replace (.getName file) ".edn" "")
               (read-string (slurp file))]))
       (into {})))

(defn- start-sites! []
  (timbre/info "Starting sites")
  (->> (get-sites)
       (map (fn [[site site-config]]
              [site
               (let [config (atom (common.utils/deep-merge @ventas.config/config site-config))]
                 (with-bindings {#'ventas.config/config config}
                   (merge
                    {#'ventas.config/config config
                     #'current site}
                    (when (get-in site [:database :url])
                      {#'ventas.database/db (db/start-db!)}))))]))
       (into {})))

(defn- stop-sites! [sites]
  (timbre/info "Stopping sites")
  (doseq [[site bindings] sites]
    (when-let [db (get bindings #'ventas.database/db)]
      (db/stop-db! db))))

(defstate sites
  :start (start-sites!)
  :stop (stop-sites! sites))

(defn with-site [server-name f]
  (if-let [site (get sites server-name)]
    (with-bindings site
      (f))
    (f)))

(defn wrap-multisite
  [handler]
  (fn [{:keys [server-name] :as req}]
    (with-site server-name #(handler req))))
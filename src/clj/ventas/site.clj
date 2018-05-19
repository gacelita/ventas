(ns ventas.site
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.paths :as paths]
   [ventas.utils :as utils]))

(def ^:dynamic current)

(defn get-sites []
  (->> (paths/resolve ::paths/sites)
       (io/file)
       (file-seq)
       (remove #(.isDirectory %))
       (utils/mapm (fn [file]
                     [(str/replace (.getName file) ".edn" "")
                      (read-string (slurp file))]))))

(defn- start-sites! []
  (timbre/info "Starting sites")
  (->> (get-sites)
       (utils/mapm
        (fn [[site site-config]]
          [site
           (let [config (atom (common.utils/deep-merge @ventas.config/config site-config))]
             (with-bindings {#'ventas.config/config config}
               (merge
                {#'ventas.config/config config
                 #'current site}
                (when (get-in site [:database :url])
                  {#'ventas.database/db (db/start-db!)}))))]))))

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

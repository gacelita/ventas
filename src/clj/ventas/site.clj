(ns ventas.site
  (:require
   [mount.core :refer [defstate]]
   [datomic.api :as d]
   [ventas.database :as db]
   [clojure.string :as str]
   [ventas.database.entity :as entity])
  (:import [datomic Datom]))

(def ^:dynamic *current* nil)

(defn site-db [site]
  (d/filter (d/db db/conn)
            (fn [_ ^Datom datom]
              (let [entity (datomic.api/entity (d/db db/conn) (.e datom))
                    entity-site (get-in entity [:ventas/site :db/id])]
                (or (not entity-site)
                    (= entity-site site))))))

(defn by-hostname [hostname]
  (when-let [subdomain (some-> hostname
                               (str/split #"\.")
                               first)]
    (entity/query-one :site {:subdomain subdomain})))

(defn with-site [hostname f]
  (if-let [site (by-hostname hostname)]
    (with-bindings {#'ventas.database/db #(site-db (:db/id site))
                    #'*current* (:db/id site)}
      (f))
    (f)))

(defn wrap-multisite
  [handler]
  (fn [{:keys [server-name] :as req}]
    (with-site server-name #(handler req))))
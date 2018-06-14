(ns ventas.site
  (:require
   [mount.core :refer [defstate]]
   [datomic.api :as d]
   [ventas.database :as db]
   [clojure.string :as str]
   [ventas.database.entity :as entity])
  (:import [datomic Datom]))

(defn site-db [site]
  (let [site-ident (d/entid (d/db db/conn) :ventas/site)
        site-filter (fn [_ ^Datom datom]
                      (or (not= site-ident (.a datom))
                          (not (.v datom))
                          (= site (.v datom))))]
    (d/filter (d/db db/conn) site-filter)))

(defn with-site [server-name f]
  (let [subdomain (first (str/split server-name #"."))
        site (entity/query-one :site {:subdomain subdomain})]
    (if site
      (with-bindings {#'ventas.database/db #(site-db (:db/id site))}
        (f))
      (f))))

(defn wrap-multisite
  [handler]
  (fn [{:keys [server-name] :as req}]
    (with-site server-name #(handler req))))
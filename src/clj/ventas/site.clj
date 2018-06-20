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

(defn by-hostname [hostname]
  (let [subdomain (first (str/split hostname #"."))]
    (entity/query-one :site {:subdomain subdomain})))

(defn with-site [hostname f]
  (if-let [site (by-hostname hostname)]
    (with-bindings {#'ventas.database/db #(site-db (:db/id site))}
      (f))
    (f)))

(defn wrap-multisite
  [handler]
  (fn [{:keys [server-name] :as req}]
    (with-site server-name #(handler req))))
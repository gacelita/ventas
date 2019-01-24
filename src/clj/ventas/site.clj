(ns ventas.site
  (:require
   [mount.core :refer [defstate]]
   [datomic.api :as d]
   [ventas.database :as db]
   [clojure.string :as str]
   [ventas.database.entity :as entity])
  (:import [datomic Datom]))

(def ^:dynamic *current* nil)

(def shared-types
  #{:schema.type/country
    :schema.type/state
    :schema.type/site
    :schema.type/currency
    :schema.type/i18n.culture})

(defn site-db [site]
  (d/filter (d/db db/conn)
            (fn [_ ^Datom datom]
              (let [entity (d/entity (d/db db/conn) (.e datom))
                    entity-site (get-in entity [:ventas/site :db/id])
                    type (get-in entity [:schema/type])]
                (or (contains? shared-types type)
                    (= entity-site site))))))

(defn by-hostname [hostname]
  (when-let [subdomain (some-> hostname
                               (str/split #"\.")
                               first)]
    (entity/query-one :site {:subdomain subdomain})))

(defn transact
  "Adds :ventas/site if needed"
  [items]
  (db/transact*
   (map (fn [item]
          (if-not (map? item)
            item
            (let [type (:schema/type item)
                  ref (db/normalize-ref type)
                  ident (db/ident ref)]
              (if (or (not *current*) (contains? shared-types ident))
                item
                (assoc item :ventas/site *current*)))))
        items)))

(defn with-site [site-id f]
  (with-bindings {#'db/db #(site-db site-id)
                  #'*current* site-id
                  #'db/transact transact}
    (f)))

(defn with-hostname [hostname f]
  (if-let [site (by-hostname hostname)]
    (with-site (:db/id site) f)
    (f)))

(defn wrap-multisite
  [handler]
  (fn [{:keys [server-name] :as req}]
    (with-hostname server-name #(handler req))))
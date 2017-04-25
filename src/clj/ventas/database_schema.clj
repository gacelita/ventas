(ns ventas.database-schema
  (:require
    [datomic.api :as d]
    [io.rkn.conformity :as c]
    [ventas.config :refer [config]]
    [ventas.database :only [db]]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clj-time.core :as time]
    [clj-time.format :as time-format]
    [ventas.util :as util]
    [taoensso.timbre :as timbre :refer (trace debug info warn error)]))

(time-format/unparse (time-format/formatters :basic-date-time) (time/now))

(defn get-migrations []
  (let [files (seq (.listFiles (io/file "resources/migrations")))]
    (map (fn [file] {(keyword (.getName file)) {:txes [(read-string (slurp file))]}}) files)))

(defn generate-migration
  ([code] (generate-migration code nil))
  ([code initial-contents]
    (let [now (time/now)
          date (time-format/unparse (time-format/formatter "yyyy_MM_dd") now)
          hours (read-string (time-format/unparse (time-format/formatter "hh") now))
          minutes (read-string (time-format/unparse (time-format/formatter "mm") now))
          seconds (read-string (time-format/unparse (time-format/formatter "ss") now))
          dt-identifier (str date "_" (+ (* 60 60 hours) (* 60 minutes) seconds))]
      (spit (str "resources/migrations/" dt-identifier "_" code ".edn") initial-contents))))

(defn delete-migration [code]
  (if-let [file (first (util/find-files "resources/migrations" (re-pattern (str ".*?" code ".*?"))))]
    (.delete file)))

(defn migrate 
  ([] (migrate false))
  ([recreate]
    (let [database-url (get-in config [:database :url])
          migrations (reverse (get-migrations))]
      (when recreate
        (info "Deleting database " database-url)
        (d/delete-database database-url)
        (info "Creating database " database-url)
        (d/create-database database-url))
      (let [connection (d/connect database-url)]
        (doseq [migration migrations]
          (info "Running migration" (first (keys migration)))
          (info (c/ensure-conforms connection migration)))))))
(ns ventas.lacinia
  (:require [clojure.edn :as edn]
            [ventas.database :as db]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]
            [taoensso.timbre :as timbre :refer (trace debug info warn error)]))

(defn get-product [context arguments value]
  (let [id (read-string (:id arguments))]
    (debug "Trying to find entity: " id)
    (db/entity-find id)))

(def schema
  (-> "resources/schema.edn"
      slurp
      edn/read-string
      (attach-resolvers {:get-product get-product})
      schema/compile))

(defn query [query]
  (execute schema query nil nil))

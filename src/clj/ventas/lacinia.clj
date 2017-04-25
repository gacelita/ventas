(ns ventas.lacinia
  (:require [clojure.edn :as edn]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [com.walmartlabs.lacinia.schema :as schema]))

(defn get-hero [context arguments value]
  (let [{:keys [episode]} arguments]
    (if (= episode :NEWHOPE)
      {:id 1000
       :name "Luke"
       :home-planet "Tatooine"
       :appears-in ["NEWHOPE" "EMPIRE" "JEDI"]}
      {:id 2000
       :name "Lando Calrissian"
       :home-planet "Socorro"
       :appears-in ["EMPIRE" "JEDI"]})))

(def schema
  (-> "resources/schema.edn"
      slurp
      edn/read-string
      (attach-resolvers {:get-hero get-hero
                         :get-droid (constantly {})})
      schema/compile))

(defn query [query]
  (execute schema query nil nil))

(ns ventas.entities.configuration
  (:refer-clojure :exclude [get set])
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils :as utils]))

(spec/def :configuration/keyword ::generators/keyword)
(spec/def :configuration/value ::generators/string)

(spec/def :schema.type/configuration
  (spec/keys :req [:configuration/keyword
                   :configuration/value]))

(entity/register-type!
 :configuration
 {:attributes
  [{:db/ident :configuration/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}

   {:db/ident :configuration/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]

  :fixtures
  (fn []
    [{:configuration/keyword :site.title
      :configuration/value "Ventas Dev Store"}])})

(defn get [k-or-ks]
  (if (coll? k-or-ks)
    (->> k-or-ks
         (map (fn [id]
                (when-let [v (get id)]
                  [id v])))
         (remove nil?)
         (into {}))
    (utils/swallow
     (-> (entity/find [:configuration/keyword k-or-ks])
         :configuration/value
         read-string))))

(defn set [k v]
  (entity/create* {:schema/type :schema.type/configuration
                   :configuration/keyword k
                   :configuration/value (pr-str v)}))
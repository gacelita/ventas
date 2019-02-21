(ns ventas.entities.configuration
  "Don't use this namespace, as it's on the way out.
   Create an entity type if you want to store config in the db."
  (:refer-clojure :exclude [get])
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [slingshot.slingshot :refer [throw+]]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils :as utils]
   [ventas.common.utils :as common.utils]))

(spec/def :configuration/keyword ::generators/keyword)
(spec/def :configuration/value ::generators/string)

(spec/def :schema.type/configuration
  (spec/keys :req [:configuration/keyword]
             :opt [:configuration/value]))

(entity/register-type!
 :configuration
 {:migrations
  [[:base [{:db/ident :configuration/keyword
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one}

           {:db/ident :configuration/value
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}]]]})

(spec/def :configuration.acl/allowed-user-roles
  (spec/coll-of keyword?))

(spec/def :schema.type/configuration.acl
  (spec/keys :opt [:configuration.acl/allowed-user-roles]))

(entity/register-type!
 :configuration.acl
 {:migrations
  [[:base [{:db/ident :configuration.acl/allowed-user-roles
            :db/valueType :db.type/ref
            :ventas/refEntityType :enum
            :db/cardinality :db.cardinality/many}
           {:db/ident :configuration.acl/keyword
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}]]]})

(defn- get* [key user]
  (when (entity/db-migrated?)
    (let [{:configuration/keys [value]} (entity/query-one :configuration {:keyword key})
          {:configuration.acl/keys [allowed-user-roles]} (entity/find [:configuration.acl/keyword key])]
      (when (and (seq allowed-user-roles)
                 (not (set/subset? (set (:user/roles user))
                                   (set allowed-user-roles))))
        (throw+ {:type ::access-denied
                 :key key}))
      (utils/swallow
       (read-string value)))))

(defn get
  "Gets a configuration key or a collection of configuration keys.
   `user` will be used for checking the roles when the configuration key has them defined."
  [k-or-ks & [user]]
  (if (coll? k-or-ks)
    (->> k-or-ks
         (map (fn [id]
                (when-let [v (get id)]
                  [id v])))
         (remove nil?)
         (into {}))
    (get* k-or-ks user)))

(defn set! [k v]
  {:pre [(keyword? k)]}
  "Sets to `v` the `k` configuration key."
  (let [value (pr-str v)]
    (if-let [existing-entity (entity/query-one :configuration {:configuration/keyword k})]
      (entity/update* (assoc existing-entity :configuration/value value))
      (entity/create*
       (common.utils/remove-nil-vals
        {:schema/type :schema.type/configuration
         :configuration/keyword k
         :configuration/value (pr-str v)})))))

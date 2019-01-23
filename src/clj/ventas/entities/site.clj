(ns ventas.entities.site
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]))

(spec/def :schema.type/site
  (spec/keys :req [:site/subdomain]))

(entity/register-type!
 :site
 {:migrations
  [[:base [{:db/ident :site/subdomain
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}]]]

  :autoresolve? true})

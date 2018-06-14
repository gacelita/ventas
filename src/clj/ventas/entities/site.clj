(ns ventas.entities.site
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]))

(spec/def :site/user ::entities.i18n/ref)

(spec/def :schema.type/site
  (spec/keys :req [:site/user
                   :site/subdomain]))

(entity/register-type!
 :site
 {:attributes
  [{:db/ident :site/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :ventas/refEntityType :user}
   {:db/ident :site/subdomain
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}]

  :autoresolve? true})

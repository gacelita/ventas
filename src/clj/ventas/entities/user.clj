(ns ventas.entities.user
  (:require [clojure.spec.alpha :as spec]
            [buddy.hashers :as hashers]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.util :as util]))

(spec/def :user/name string?)

(spec/def :user/password string?)

(spec/def :user/description string?)

(spec/def :user/status
  #{:user.status/pending
    :user.status/active
    :user.status/inactive
    :user.status/cancelled})

(spec/def :user/roles
  (spec/coll-of #{:user.role/administrator
                  :user.role/user}
                :kind set?))

(spec/def :user/email
  (spec/with-gen
   (spec/and string? #(re-matches #"^.+@.+$" %))
   #(gen'/string-from-regex #"[a-z0-9]{3,6}@[a-z0-9]{3,6}\.(com|es|org)")))

(spec/def :schema.type/user
  (spec/keys :req [:user/name
                   :user/password
                   :user/email
                   :user/status]
             :opt [:user/description
                   :user/roles]))

(entity/register-type! :user
 {:attributes
  [{:db/ident :user/name
    :db/valueType :db.type/string
    :db/fulltext true
    :db/index true
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/password
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/email
    :db/valueType :db.type/string
    :db/index true
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/avatar
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :user.status/pending}
   {:db/ident :user.status/active}
   {:db/ident :user.status/inactive}
   {:db/ident :user.status/cancelled}

   {:db/ident :user/roles
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :user.role/administrator}
   {:db/ident :user.role/user}]

  :filter-transact
  (fn [this]
    (util/transform
     this
     [#(update
        %
        :user/password
        (fn [v]
          (when v (hashers/derive v))))]))})
(ns ventas.entities.user
  (:require
   [buddy.hashers :as hashers]
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [com.gfredericks.test.chuck.generators :as chuck]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils :as utils]))

(spec/def :user/first-name ::generators/string)

(spec/def :user/password ::generators/string)

(spec/def :user/description ::generators/string)

(spec/def :user/first-name ::generators/string)

(spec/def :user/last-name ::generators/string)

(spec/def :user/company ::generators/string)

(spec/def :user/phone ::generators/string)

(def statuses
  #{;; user needs approval
    :user.status/pending
    ;; OK
    :user.status/active
    ;; disabled but not cancelled
    :user.status/inactive
    ;; terminated, killed, banned
    :user.status/cancelled
    ;; temporary user that should register at some point in time
    :user.status/unregistered})

(spec/def :user/status
  (entity/enum-spec statuses))

(def roles
  #{:user.role/administrator
    :user.role/user})

(spec/def ::role
  (spec/or :pull-eid ::db/pull-eid
           :role roles))

(spec/def :user/roles
  (spec/coll-of ::role))

(def cultures
  #{:user.culture/en_US
    :user.culture/es_ES})

(spec/def :user/culture
  (spec/with-gen ::entity/ref #(entity/ref-generator :i18n.culture)))

(spec/def :user/favorites
  (spec/with-gen ::entity/refs #(entity/refs-generator :product.variation)))

(spec/def :user/email
  (spec/with-gen
    (spec/and string? #(str/includes? % "@"))
    #(chuck/string-from-regex #"[a-z0-9]{3,6}@[a-z0-9]{3,6}\.(com|es|org)")))

(spec/def :schema.type/user
  (spec/keys :opt [:user/description
                   :user/roles
                   :user/password
                   :user/favorites
                   :user/first-name
                   :user/last-name
                   :user/company
                   :user/email
                   :user/phone
                   :user/status
                   :user/culture]))

(defn get-name [id-or-entity]
  {:pre [id-or-entity]}
  (let [{:user/keys [first-name last-name]}
        (if (entity/entity? id-or-entity) id-or-entity (entity/find id-or-entity))]
    (str/join " " [first-name last-name])))

(def default-culture [:i18n.culture/keyword :en_US])

(entity/register-type!
 :user
 {:migrations
  [[:base [{:db/ident :user/first-name
            :db/valueType :db.type/string
            :db/fulltext true
            :db/index true
            :db/cardinality :db.cardinality/one}

           {:db/ident :user/last-name
            :db/valueType :db.type/string
            :db/fulltext true
            :db/index true
            :db/cardinality :db.cardinality/one}

           {:db/ident :user/company
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

           {:db/ident :user/phone
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}

           {:db/ident :user/description
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}

           {:db/ident :user/avatar
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}

           {:db/ident :user/status
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one
            :ventas/refEntityType :enum}

           {:db/ident :user/culture
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}

           {:db/ident :user/roles
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many
            :ventas/refEntityType :enum}

           {:db/ident :user.role/administrator}

           {:db/ident :user/favorites
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
           {:db/ident :user.status/pending}
           {:db/ident :user.status/active}
           {:db/ident :user.status/inactive}
           {:db/ident :user.status/cancelled}
           {:db/ident :user.status/unregistered}]]
   [:add-user-role [{:db/ident :user.role/user}]]]

  :serialize
  (fn [this params]
    (-> ((entity/default-attr :serialize) this params)
        (dissoc :password)
        (dissoc :favorites)
        (assoc :name (get-name (:db/id this)))))

  :filter-create
  (fn [this]
    (cond-> this
            (:user/password this) (update :user/password hashers/derive)
            (not (:user/culture this)) (assoc :user/culture default-culture)))})

(defn get-cart
  "Gets the user's cart if it exists, creates it otherwise"
  [{:db/keys [id]}]
  {:pre [id]}
  (if-let [cart (entity/query-one :order {:status :order.status/draft
                                          :user id})]
    cart
    (entity/create :order {:status :order.status/draft
                           :user id})))

(defn get-culture [user]
  {:pre [(or (not user) (utils/check :schema.type/user user))]}
  (or (:user/culture user)
      (:db/id (db/entity default-culture))))
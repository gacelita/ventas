(ns ventas.entities.user
  (:require [clojure.spec :as s]
            [buddy.hashers :as hashers]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :user/name string?)
(s/def :user/password string?)
(s/def :user/description string?)
(s/def :user/status #{:user.status/pending :user.status/active :user.status/inactive :user.status/cancelled})
(s/def :user/roles #{:user.role/administrator :user.role/user})
(s/def :user/email
  (s/with-gen (s/and string? #(re-matches #"^.+@.+$" %))
              #(gen'/string-from-regex #"[a-z0-9]{3,6}@[a-z0-9]{3,6}\.(com|es|org)")))

(s/def :schema.type/user
  (s/keys :req [:user/name :user/password :user/email :user/status]
          :opt [:user/description :user/roles]))

(defmethod entity/precreate :user [data]
  (as-> data data
        (if-not (:user/status data)
          (assoc data :user/status :user.status/active)
          data)
        (update data :user/password hashers/derive)))

(defmethod entity/preupdate :user [type entity data]
  (update data :password hashers/derive))

(ns ventas.entities.address
  (:require [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(s/def :address/name string?)
(s/def :address/person-name string?)
(s/def :address/address string?)
(s/def :address/zip integer?)
(s/def :address/city string?)
(s/def :address/country
  (s/with-gen integer? #(gen/elements (map :id (entity/query :country)))))
(s/def :address/state
  (s/with-gen integer? #(gen/elements (map :id (entity/query :state)))))
(s/def :address/user
  (s/with-gen integer? #(gen/elements (map :id (entity/query :user)))))
(s/def :address/phone string?)
(s/def :address/comments string?)


(s/def :schema.type/address
  (s/keys :req [:address/name
                :address/person-name
                :address/address
                :address/zip
                :address/city
                :address/country
                :address/state
                :address/user]
          :opt [:address/phone
                :address/comments]))

(defmethod entity/json :address [entity]
  (as-> entity entity
        (dissoc entity :type)
        (if-let [country (:country entity)]
          (assoc entity :country (entity/json (entity/find country)))
          entity)
        (if-let [state (:state entity)]
          (assoc entity :state (entity/json (entity/find state)))
          entity)
        (if-let [user (:user entity)]
          (assoc entity :user (entity/json (entity/find user)))
          entity)))
(ns ventas.entities.address
  (:require [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [ventas.database :as db]
            [ventas.database.entity :as entity]))

(spec/def :address/name string?)
(spec/def :address/person-name string?)
(spec/def :address/address string?)
(spec/def :address/zip integer?)
(spec/def :address/city string?)
(spec/def :address/country
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :country)))))
(spec/def :address/state
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :state)))))
(spec/def :address/user
  (spec/with-gen integer? #(gen/elements (map :id (entity/query :user)))))
(spec/def :address/phone string?)
(spec/def :address/comments string?)


(spec/def :schema.type/address
  (spec/keys :req [:address/name
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
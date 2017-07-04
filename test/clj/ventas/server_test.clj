(ns ventas.server-test
  (:require [clojure.test :refer :all]
            [ventas.server :as server]
            [ventas.database :as db]))

(deftest entities-remove
    (let [user (db/entity-create :user (db/generate-1 :schema.type/user))
          result (server/ws-request-handler {:name :entities.remove :params {:id (:id user)}} {})]
        (is (= result (:id user)))))

(deftest entities-find
    (let [user (db/entity-create :user (db/generate-1 :schema.type/user))
          result (server/ws-request-handler {:name :entities.find :params {:id (:id user)}} {})]
        (is (= result (db/entity-find (:id user))))))

(deftest users-save
    (let [user (db/entity-create :user (db/generate-1 :schema.type/user))
          result (server/ws-request-handler {:name :users.save :params (assoc user :name "MODIFIED")} {})]
        (is (=  (db/entity-find (:id user)) (assoc user :name "MODIFIED")))))

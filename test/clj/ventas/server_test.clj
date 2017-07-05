(ns ventas.server-test
  (:require [clojure.test :refer :all]
            [ventas.server :as server]
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [ventas.database.seed :as seed]))

(deftest entities-remove
    (let [user (entity/create :user (seed/generate-1 :schema.type/user))
          result (server/ws-request-handler {:name :entities.remove :params {:id (:id user)}} {})]
        (is (= result (:id user)))))

(deftest entities-find
    (let [user (entity/create :user (seed/generate-1 :schema.type/user))
          result (server/ws-request-handler {:name :entities.find :params {:id (:id user)}} {})]
        (is (= result (entity/find (:id user))))))

(deftest users-save
    (let [user (entity/create :user (seed/generate-1 :schema.type/user))
          result (server/ws-request-handler {:name :users.save :params (assoc user :name "MODIFIED")} {})]
        (is (=  (entity/find (:id user)) (assoc user :name "MODIFIED")))))

(ns ventas.server-test
  (:require [clojure.test :refer :all]
            [ventas.server :as server]
            [ventas.database :as db]))

(comment 


    (defmethod ws-request-handler :users.list [message state]
      (db/entity-query :user))

    (defmethod ws-request-handler :users.made-comments.list [message state]
      (db/entity-query :comment {:source (get-in message [:params :id])}))

    (defmethod ws-request-handler :users.comments.list [message state]
      (db/entity-query :comment {:target (get-in message [:params :id])}))

    (defmethod ws-request-handler :users.friends.list [message state]
        (concat (map :source (db/entity-query :friendship {:target (get-in message [:params :id])}))
                (map :target (db/entity-query :friendship {:source (get-in message [:params :id])}))))

    (defmethod ws-request-handler :users.save [message state]
      (db/entity-upsert :user (:params message))))



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

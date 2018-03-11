(ns ventas.auth-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.auth :as sut]
   [ventas.test-tools :as test-tools]))

(use-fixtures :once
              #(with-redefs [db/db (test-tools/test-conn)]
                 (%)))

(def test-user-attrs
  {:email "email@email.com"})

(deftest user->token-and-token->user
  (let [user (entity/create :user test-user-attrs)
        token (sut/user->token user)]
    (is (= (sut/token->user token) user))))
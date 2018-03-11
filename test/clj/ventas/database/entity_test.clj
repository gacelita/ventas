(ns ventas.database.entity-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.database :as db]
   [ventas.database.entity :as sut]
   [ventas.test-tools :as test-tools]))

(deftest entity?
  (is (= (sut/entity? {:schema/type :any-kw}) true))
  (is (= (sut/entity? {}) false))
  (is (= (sut/entity? {:type :any-kw}) false)))

(def test-user-attrs
  {:first-name "Test user"
   :password ""
   :email "email@email.com"
   :status :user.status/active})

(deftest entity-create
  (with-redefs [db/db (test-tools/test-conn)]
    (let [user (sut/create :user test-user-attrs)]
      (is (= (-> user
                 (dissoc :db/id)
                 (dissoc :user/password))
             {:schema/type :schema.type/user
              :user/first-name "Test user"
              :user/email "email@email.com"
              :user/status :user.status/active}))
      (sut/delete (:db/id user)))))

(deftest register-type!
  (with-redefs [sut/registered-types (atom {})]
    (is (do (sut/register-type! :new-type)
            (= @sut/registered-types {:new-type {}})))
    (is (let [test-fns {:filter-json (fn [])}]
          (sut/register-type! :new-type test-fns)
          (= @sut/registered-types {:new-type test-fns})))))

(deftest entities-remove
  (with-redefs [db/db (test-tools/test-conn)]
    (let [{id :db/id} (sut/create :user test-user-attrs)]
      (sut/delete id)
      (is (= nil (sut/find id))))))

(deftest entities-find
  (with-redefs [db/db (test-tools/test-conn)]
    (let [{id :db/id :as user} (sut/create :user test-user-attrs)]
      (is (= user (sut/find id)))
      (sut/delete id))))

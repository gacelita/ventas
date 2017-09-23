(ns ventas.entity-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.database.entity :as sut]
   [ventas.database.seed :as seed]))

(deftest is-entity?
  (is (= (sut/is-entity? {:schema/type :any-kw}) true))
  (is (= (sut/is-entity? {}) false))
  (is (= (sut/is-entity? {:type :any-kw}) false)))

(def test-user-attrs
  {:name "Test user"
   :password ""
   :email "email@email.com"
   :status :user.status/active})

(deftest entity-create
  (let [user (sut/create :user test-user-attrs)]
    (is (= (-> user
               (dissoc :db/id)
               (dissoc :user/password))
           {:schema/type :schema.type/user
            :user/name "Test user"
            :user/email "email@email.com"
            :user/status :user.status/active}))
    (sut/delete (:db/id user))))

(deftest register-type!
  (with-redefs [sut/registered-types (atom {})]
    (is (do (sut/register-type! :new-type)
            (= @sut/registered-types {:new-type {}})))
    (is (let [test-fns {:filter-json (fn [])}]
          (sut/register-type! :new-type test-fns)
          (= @sut/registered-types {:new-type test-fns})))))

(deftest entities-remove
  (let [{id :db/id} (sut/create :user test-user-attrs)]
    (sut/delete id)
    (is (= nil (sut/find id)))))

(deftest entities-find
  (let [{id :db/id :as user} (sut/create :user test-user-attrs)]
    (is (= user (sut/find id)))
    (sut/delete id)))

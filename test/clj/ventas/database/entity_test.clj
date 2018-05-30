(ns ventas.database.entity-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [taoensso.timbre :as timbre]
   [ventas.database :as db]
   [ventas.database.entity :as sut]
   [ventas.test-tools :as test-tools]))

(use-fixtures :each #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level
                        :report
                        (%))))

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
  (let [user (sut/create :user test-user-attrs)]
    (is (= (-> user
               (dissoc :db/id)
               (dissoc :user/password))
           {:schema/type :schema.type/user
            :user/first-name "Test user"
            :user/email "email@email.com"
            :user/status :user.status/active}))
    (sut/delete (:db/id user))))

(deftest register-type!
  (with-redefs [sut/registered-types (atom {})]
    (is (do (sut/register-type! :new-type {:attributes []})
            (= @sut/registered-types {:new-type {:attributes []}})))
    (is (let [properties {:filter-json (fn [])
                          :attributes []}]
          (sut/register-type! :new-type properties)
          (= @sut/registered-types {:new-type properties})))))

(deftest entities-remove
  (let [{id :db/id} (sut/create :user test-user-attrs)]
    (sut/delete id)
    (is (= nil (sut/find id)))))

(deftest entities-find
  (let [{id :db/id :as user} (sut/create :user test-user-attrs)]
    (is (= user (sut/find id)))
    (sut/delete id)))

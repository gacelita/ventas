(ns ventas.server.api.admin-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.test-tools :as test-tools]
   [taoensso.timbre :as timbre]
   [ventas.database.seed :as seed]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.server.api.admin :as sut]
   [ventas.server.api :as api]
   [ventas.server.ws :as server.ws]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.entities.configuration :as entities.configuration]))

(defn example-user []
  {:schema/type :schema.type/user
   :user/first-name "Test user"
   :user/roles #{:user.role/administrator}
   :user/email (str (gensym "test-user") "@test.com")})

(declare user)

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level
                        :report
                        (seed/seed :minimal? true)
                        (with-redefs [user (entity/create* (example-user))]
                          (%)))))

(deftest register-admin-endpoint!
  (#'sut/register-admin-endpoint!
   ::test
   (fn [_ {:keys [session]}]
     (is (= user (api/get-user session)))))
  (is (server.ws/call-handler-with-user ::test {} user))
  (is (= {:data "This API request requires administration privileges"
          :id nil
          :success false
          :type :response}
         (server.ws/call-request-handler {:name ::test}
                                         {:session (atom {})}))))

(def example-brand
  {:brand/name (entities.i18n/get-i18n-entity {:en_US "Example brand"})
   :brand/description (entities.i18n/get-i18n-entity {:en_US "The best brand ever"})
   :brand/keyword :example-brand
   :schema/type :schema.type/brand})

(deftest admin-entities-find-json
  (is (= (entity/to-json user {:culture [:i18n.culture/keyword :en_US]})
         (-> (server.ws/call-handler-with-user :admin.entities.find-json {:id (:db/id user)} user)
             :data))))

(deftest admin-entities-pull
  (is (= (db/pull '[*] (:db/id user))
         (-> (server.ws/call-handler-with-user :admin.entities.pull {:id (:db/id user)} user)
             :data)))
  (is (= (db/pull '[* {:user/culture [*]}] (:db/id user))
         (-> (server.ws/call-handler-with-user :admin.entities.pull {:id (:db/id user)} user)
             :data))))

(deftest admin-entities-save
  (let [new-company "New Company Inc."]
    (server.ws/call-handler-with-user :admin.entities.save
                                      (-> (db/pull '[*] (:db/id user))
                                          (assoc :user/company new-company))
                                      user)
    (is (= new-company
           (:user/company (entity/find (:db/id user)))))))

(deftest admin-entities-remove
  (let [new-entity (entity/create :user {:first-name "Test user"})]
    (server.ws/call-handler-with-user :admin.entities.remove
                                      {:id (:db/id new-entity)}
                                      user)
    (is (not (entity/find (:db/id new-entity))))))

(deftest admin-entities-list
  (let [brand (entity/create* example-brand)]
    (is (= [(-> (entity/to-json brand {:culture [:i18n.culture/keyword :en_US]})
                (dissoc :id))]
           (->> (server.ws/call-handler-with-user :admin.entities.list {:type :brand} user)
                :data
                (map #(dissoc % :id)))))))

(deftest admin-plugins-list
  (with-redefs [ventas.plugin/plugins (atom {:test {:name "Test plugin"
                                                    :migrations []}
                                             :theme {:name "Test theme"
                                                     :type :theme}})]
    (is (= [{:name "Test theme"}
            {:name "Test plugin"}]
           (-> (server.ws/call-handler-with-user :admin.plugins.list {} user)
               :data)))))

(deftest admin-configuration-get
  (let [test-data {:test1 "value1"
                   :test2 "value2"}]
    (doseq [[k v] test-data]
      (entities.configuration/set! k v))
    (is (= test-data
           (-> (server.ws/call-handler-with-user :admin.configuration.get (keys test-data) user)
               :data)))))

(deftest admin-configuration-set
  (server.ws/call-handler-with-user :admin.configuration.set {:test3 "value3"} user)
  (is (= "value3" (entities.configuration/get :test3))))
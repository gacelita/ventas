(ns ventas.server.api.admin-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.seed :as seed]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.server.api :as api]
   [ventas.server.api.admin :as sut]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]))

(defn example-user []
  {:schema/type :schema.type/user
   :user/first-name "Test user"
   :user/roles #{:user.role/administrator}
   :user/email (str (gensym "test-user") "@test.com")})

(declare user)

(use-fixtures :once #(test-tools/with-test-context
                       (seed/seed :minimal? true)
                       (with-redefs [user (entity/create* (example-user))]
                         (%))))

(deftest register-admin-endpoint!
  (#'sut/register-admin-endpoint!
   ::test
   (fn [_ {:keys [session]}]
     (is (= user (api/get-user session)))))
  (is (server.ws/call-handler-with-user ::test {} user))
  (is (= ::sut/unauthorized
         (-> (server.ws/call-request-handler {:name ::test}
                                             {:session (atom {})})
             :data
             :type))))

(def example-brand
  {:brand/name (entities.i18n/get-i18n-entity {:en_US "Example brand"})
   :brand/description (entities.i18n/get-i18n-entity {:en_US "The best brand ever"})
   :brand/keyword :example-brand
   :schema/type :schema.type/brand})

(deftest admin-entities-find-serialize
  (is (= (entity/serialize user {:culture [:i18n.culture/keyword :en_US]})
         (-> (server.ws/call-handler-with-user :admin.entities.find-serialize {:id (:db/id user)} user)
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
    (is (= [(-> (entity/serialize brand {:culture [:i18n.culture/keyword :en_US]})
                (dissoc :id))]
           (->> (server.ws/call-handler-with-user :admin.entities.list {:type :brand} user)
                :data
                (map #(dissoc % :id)))))))

(deftest admin-plugins-list
  (with-redefs [ventas.plugin/plugins (atom {:test {:name "Test plugin"
                                                    :migrations []}
                                             :theme {:name "Test theme"
                                                     :type :theme}})]
    (is (= [{:id :test :name "Test plugin"}
            {:id :theme :name "Test theme" :type :theme}]
           (-> (server.ws/call-handler-with-user :admin.plugins.list {} user)
               :data)))))

(deftest admin-configuration-set
  (server.ws/call-handler-with-user :admin.configuration.set {:test3 "value3"} user)
  (is (= "value3" (entities.configuration/get :test3))))

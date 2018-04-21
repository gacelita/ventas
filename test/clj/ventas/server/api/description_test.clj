(ns ventas.server.api.description-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [spec-tools.data-spec :as data-spec]
   [taoensso.timbre :as timbre]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.server.api :as api]
   [ventas.server.pagination :as pagination]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]
   [ventas.utils :as utils]))

(defn example-user []
  {:schema/type :schema.type/user
   :user/first-name "Test user"
   :user/roles #{:user.role/administrator}
   :user/email (str (gensym "test-user") "@test.com")})

(declare user)

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level
                        :report
                        (with-redefs [user (entity/create* (example-user))]
                          (%)))))

(deftest describe
  (with-redefs [api/available-requests (atom {:test {:binary? false
                                                     :spec {:some integer?
                                                            :thing string?
                                                            :stuff [integer?]}
                                                     :doc "Documentation!"
                                                     :middlewares [pagination/paginate]}})]
    (is (= {:test {:doc "Documentation!"
                   :spec {:keys {:some {:type :number}
                                 :stuff {:items {:type :number} :type :vector}
                                 :thing {:type :string}}
                          :required [:some :thing :stuff]
                          :type :map}}}
           (-> (server.ws/call-handler-with-user :api.describe {} user)
               :data)))))

(deftest generate-params
  (dotimes [n 10]
    (is (utils/check
         (data-spec/spec :products.aggregations (get-in @api/available-requests [:products.aggregations :spec]))
         (-> (server.ws/call-handler-with-user :api.generate-params
                                               {:request :products.aggregations}
                                               user)
             :data)))))

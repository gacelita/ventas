(ns ventas.server.api.description-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [spec-tools.data-spec :as data-spec]
   [ventas.database.entity :as entity]
   [ventas.server.api :as api]
   [ventas.server.pagination :as pagination]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]
   [ventas.server.api.description :as sut]
   [ventas.utils :as utils]))

(defn example-user []
  {:schema/type :schema.type/user
   :user/first-name "Test user"
   :user/roles #{:user.role/administrator}
   :user/email (str (gensym "test-user") "@test.com")})

(declare user)

(use-fixtures :once #(test-tools/with-test-context
                      (with-redefs [user (entity/create* (example-user))]
                        (%))))

(deftest describe
  (with-redefs [api/available-requests (atom {:test {:binary? false
                                                     :spec {:some integer?
                                                            :thing string?
                                                            :stuff [integer?]}
                                                     :doc "Documentation!"
                                                     :middlewares [pagination/paginate]}})]
    (is (= {:test {:doc "Documentation!"
                   :spec {:keys {:some {:type :number}
                                 :stuff {:items {:type :number
                                                 :title :api$test/stuff} :type :vector}
                                 :thing {:type :string}}
                          :required [:some :thing :stuff]
                          :title :api/test
                          :type :map}}}
           (-> (server.ws/call-handler-with-user ::sut/api.describe {} user)
               :data)))))

(deftest generate-params
  (dotimes [n 10]
    (is (utils/check
         (data-spec/spec :api/products.aggregations (get-in @api/available-requests [::api/products.aggregations :spec]))
         (:data
          (server.ws/call-handler-with-user ::sut/api.generate-params
                                            {:request :products.aggregations}
                                            user))))))

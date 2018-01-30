(ns ventas.server.api-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.database :as db]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]
   [ventas.database.seed :as seed]))

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (seed/seed :minimal? true)
                       (%)))

(deftest products-aggregations
  (testing "works without passing params"
    (is (= {:data {:can-load-more? false :items [] :taxonomies []}
            :id nil
            :success true
            :type :response}
           (server.ws/call-request-handler {:name :products.aggregations}
                                                  {})))))

(deftest products-list
  (testing "works without passing params"
    (is (= {:data [] :id nil :success true :type :response}
           (server.ws/call-request-handler {:name :products.list}
                                                  {})))))
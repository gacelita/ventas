(ns ventas.server.api-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.database :as db]
   [ventas.database.seed :as seed]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]))

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (seed/seed :minimal? true)
                       (%)))

(deftest products-aggregations
  (testing "spec does not fail when not passing params"
    (is (= {:data "Elasticsearch error: "
            :id nil
            :success false
            :type :response}
           (server.ws/call-request-handler {:name :products.aggregations}
                                           {})))))

(deftest products-list
  (testing "works without passing params"
    (is (= {:data [] :id nil :success true :type :response}
           (server.ws/call-request-handler {:name :products.list}
                                           {})))))

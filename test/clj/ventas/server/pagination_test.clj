(ns ventas.server.pagination-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ventas.server.pagination :as sut]))

(deftest wrap-paginate
  (let [request {:params {:pagination {:items-per-page 2
                                       :page 2}}}]
    (is (= {:request request
            :response {:items [4]
                       :total 5}}
           (sut/wrap-paginate {:request request
                               :response (range 5)})))))

(deftest wrap-sort
  (let [request {:params {:sorting {:field :name
                                    :direction :asc}}}
        items [{:name :x :other :field}
               {:name :yy :other :field}
               {:name :c :other :field}]]
    (testing "sorts"
      (is (= {:request request
              :response (sort-by :name items)}
             (sut/wrap-sort {:request request
                             :response items}))))
    (testing "sorts with DESC direction"
      (let [request (assoc-in request [:params :sorting :direction] :desc)]
        (is (= {:request request
                :response (reverse (sort-by :name items))}
               (sut/wrap-sort {:request request
                               :response items})))))))

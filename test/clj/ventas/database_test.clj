(ns ventas.database-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [ventas.database :as sut]
   [ventas.database.entity :as entity]
   [ventas.test-tools :as test-tools]
   [ventas.database.seed :as seed]))

(deftest nice-query
  (let [c (test-tools/test-conn)]
    (with-redefs [sut/db c]
      (seed/seed-type-with-deps :product 1)
      (let [product (first (entity/query :product))]
        (print product)
        (is (sut/nice-query
             {:find ['?images]
              :in {'?id 17592186046637
                   '?attr :product/images}
              :where '[[?id ?attr ?images]]}))))))

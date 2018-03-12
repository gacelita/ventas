(ns ventas.server.api.user-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [taoensso.timbre :as timbre]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.seed :as seed]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.server.api.user :as sut]
   [ventas.server.api :as api]
   [ventas.core]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]
   [ventas.utils :as utils]))

(defn example-user []
  {:schema/type :schema.type/user
   :user/first-name "Test user"
   :user/email (str (gensym "test-user") "@test.com")})

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level
                        :report
                        (seed/seed :minimal? true)
                        (%))))

(defn example-address [user-email]
  {:schema/type :schema.type/address
   :address/first-name "Test"
   :address/last-name "Address"
   :address/address "Test Street, 210"
   :address/zip "67943"
   :address/city "Test City"
   :address/country (-> (entity/query :country) first :db/id)
   :address/state (-> (entity/query :state) first :db/id)
   :address/user (db/normalize-ref [:user/email user-email])})

(deftest register-user-endpoint!
  (let [user (entity/create* (example-user))]
    (#'sut/register-user-endpoint!
     ::test
     (fn [_ {:keys [session]}]
       (is (= user (api/get-user session)))))
    (is (server.ws/call-handler-with-user ::test {} user))
    (is (= {:data "This API request requires authentication"
            :id nil
            :success false
            :type :response}
           (server.ws/call-request-handler {:name ::test}
                                           {:session (atom {})})))))

(deftest users-addresses
  (let [user (entity/create* (example-user))
        address (entity/create* (example-address (:user/email user)))]
    (is (= [(entity/to-json address {:culture [:i18n.culture/keyword :en_US]})]
           (-> (server.ws/call-handler-with-user :users.addresses
                                                 {}
                                                 user)
               :data)))))

(deftest users-addresses-save
  (let [user (entity/create* (example-user))]
    (is (= (example-address (:user/email user))
           (-> (server.ws/call-handler-with-user :users.addresses.save
                                                 (-> (example-address (:user/email user))
                                                     (utils/dequalify-keywords)
                                                     (dissoc :type))
                                                 user)
               :data
               (dissoc :db/id))))))

(deftest users-addresses-remove
  (let [user (entity/create* (example-user))
        address (entity/create* (example-address (:user/email user)))]
    (is (= true
           (-> (server.ws/call-handler-with-user :users.addresses.remove
                                                 {:id (:db/id address)}
                                                 user)
               :data)))))

(def example-product
  {:schema/type :schema.type/product
   :product/name (entities.i18n/get-i18n-entity {:en_US "Example product"})
   :product/price {:schema/type :schema.type/amount
                   :amount/value 21.0M
                   :amount/currency [:currency/keyword :eur]}})

(deftest users-cart
  (let [user (entity/create* (example-user))
        product (entity/create* example-product)
        variation (entity/create :product.variation {:parent (:db/id product)
                                                     :terms #{}})
        variation-json (entity/to-json variation {:culture [:i18n.culture/keyword :en_US]})]

    (server.ws/call-handler-with-user :users.cart.add
                                      {:id (:db/id variation)}
                                      user)
    (is (= {:amount (get-in variation-json [:price :value])
            :lines [{:product-variation variation-json
                     :quantity 1}]
            :status :order.status/draft
            :user (:db/id user)}
           (-> (server.ws/call-handler-with-user :users.cart.get
                                                 {}
                                                 user)
               :data
               (dissoc :id)
               (update :lines (fn [lines] (map #(dissoc % :id) lines))))))

    (server.ws/call-handler-with-user :users.cart.remove
                                      {:id (:db/id variation)}
                                      user)

    (is (= {:amount 0
            :status :order.status/draft
            :user (:db/id user)}
           (-> (server.ws/call-handler-with-user :users.cart.get
                                                 {}
                                                 user)
               :data
               (dissoc :id))))

    (server.ws/call-handler-with-user :users.cart.set-quantity
                                      {:id (:db/id variation)
                                       :quantity 5}
                                      user)

    (is (= {:amount (* 5 (get-in variation-json [:price :value]))
            :lines [{:product-variation variation-json
                     :quantity 5}]
            :status :order.status/draft
            :user (:db/id user)}
           (-> (server.ws/call-handler-with-user :users.cart.get
                                                 {}
                                                 user)
               :data
               (dissoc :id)
               (update :lines (fn [lines] (map #(dissoc % :id) lines))))))))

(deftest users-favorites
  (let [user (entity/create* (example-user))
        product (entity/create* example-product)
        variation (entity/create :product.variation {:parent (:db/id product)
                                                     :terms #{}})
        variation-json (entity/to-json variation {:culture [:i18n.culture/keyword :en_US]})]

    (server.ws/call-handler-with-user :users.favorites.add
                                      {:id (:db/id variation)}
                                      user)

    (is (= [(:db/id variation)]
           (-> (server.ws/call-handler-with-user :users.favorites.enumerate
                                                 {}
                                                 user)
               :data)))

    (is (= [variation-json]
           (-> (server.ws/call-handler-with-user :users.favorites.list
                                                 {}
                                                 user)
               :data)))

    (server.ws/call-handler-with-user :users.favorites.remove
                                      {:id (:db/id variation)}
                                      user)

    (is (= nil
           (-> (server.ws/call-handler-with-user :users.favorites.enumerate
                                                 {}
                                                 user)
               :data)))))
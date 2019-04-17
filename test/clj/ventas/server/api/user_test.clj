(ns ventas.server.api.user-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.core]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.seed :as seed]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.server.api :as api]
   [ventas.server.api.user :as sut]
   [ventas.server.ws :as server.ws]
   [ventas.test-tools :as test-tools]
   [ventas.utils :as utils]))

(defn example-user []
  {:schema/type :schema.type/user
   :user/first-name "Test user"
   :user/email (str (gensym "test-user") "@test.com")})

(use-fixtures :once #(test-tools/with-test-context
                       (seed/seed :minimal? true)
                       (%)))

(defn example-address [user-email]
  {:schema/type :schema.type/address
   :address/first-name "Test"
   :address/last-name "Address"
   :address/address "Test Street, 210"
   :address/zip "67943"
   :address/city "Test City"
   :address/country {:schema/type :schema.type/country
                     :country/name (entities.i18n/->entity {:en_US "Test country"})
                     :country/keyword :test-country}
   :address/state {:schema/type :schema.type/state
                   :state/name (entities.i18n/->entity {:en_US "Test state"})
                   :state/keyword :test-state}
   :address/user (db/normalize-ref [:user/email user-email])})

(deftest register-user-endpoint!
  (let [user (entity/create* (example-user))]
    (#'sut/register-user-endpoint!
     ::test
     (fn [_ {:keys [session]}]
       (is (= user (api/get-user session)))))
    (is (server.ws/call-handler-with-user ::test {} user))
    (is (= ::sut/authentication-required
           (-> (server.ws/call-request-handler {:name ::test}
                                               {:session (atom {})})
               :data
               :type)))))

(deftest users-addresses
  (let [user (entity/create* (example-user))
        address (entity/create* (example-address (:user/email user)))]
    (is (= [(entity/serialize address {:culture [:i18n.culture/keyword :en_US]})]
           (-> (server.ws/call-handler-with-user ::sut/users.addresses
                                                 {}
                                                 user)
               :data)))))

(deftest users-addresses-save
  (let [user (entity/create* (example-user))]
    (is (= (-> (example-address (:user/email user))
               (dissoc :address/country :address/state)
               (entity/serialize {:culture [:i18n.culture/keyword :en_US]}))
           (-> (server.ws/call-handler-with-user ::sut/users.addresses.save
                                                 (-> (example-address (:user/email user))
                                                     (utils/dequalify-keywords)
                                                     (dissoc :type))
                                                 user)
               :data
               (dissoc :id :country :state))))))

(deftest users-addresses-remove
  (let [user (entity/create* (example-user))
        address (entity/create* (example-address (:user/email user)))]
    (is (= true
           (-> (server.ws/call-handler-with-user ::sut/users.addresses.remove
                                                 {:id (:db/id address)}
                                                 user)
               :data)))))

(def example-product
  {:schema/type :schema.type/product
   :product/name (entities.i18n/->entity {:en_US "Example product"})
   :product/price {:schema/type :schema.type/amount
                   :amount/value 21.0M
                   :amount/currency [:currency/keyword :eur]}})

(deftest users-cart
  (let [user (entity/create* (example-user))
        product (entity/create* example-product)
        variation (entity/create :product.variation {:parent (:db/id product)
                                                     :terms #{}})
        serialized-variation (entity/serialize variation {:culture [:i18n.culture/keyword :en_US]})]

    (testing "cart add"
      (server.ws/call-handler-with-user ::sut/users.cart.add
                                        {:id (:db/id variation)}
                                        user)
      (is (= {:amount (:price serialized-variation)
              :lines [{:product-variation serialized-variation
                       :quantity 1}]
              :status {:ident :order.status/draft
                       :id (db/entid :order.status/draft)
                       :name "Draft"}
              :user (:db/id user)}
             (-> (server.ws/call-handler-with-user ::sut/users.cart.get
                                                   {}
                                                   user)
                 :data
                 (dissoc :id)
                 (update :lines (fn [lines] (map #(dissoc % :id) lines)))))))

    (testing "cart remove"
      (server.ws/call-handler-with-user ::sut/users.cart.remove
                                        {:id (:db/id variation)}
                                        user)
      (is (= {:amount nil
              :status {:ident :order.status/draft
                       :id (db/entid :order.status/draft)
                       :name "Draft"}
              :user (:db/id user)}
             (-> (server.ws/call-handler-with-user ::sut/users.cart.get
                                                   {}
                                                   user)
                 :data
                 (dissoc :id)))))

    (testing "cart set-quantity"
      (server.ws/call-handler-with-user ::sut/users.cart.set-quantity
                                        {:id (:db/id variation)
                                         :quantity 5}
                                        user)
      (is (= {:amount (-> (:price serialized-variation)
                          (update :value #(* 5 %)))
              :lines [{:product-variation serialized-variation
                       :quantity 5}]
              :status {:ident :order.status/draft
                       :id (db/entid :order.status/draft)
                       :name "Draft"}
              :user (:db/id user)}
             (-> (server.ws/call-handler-with-user ::sut/users.cart.get
                                                   {}
                                                   user)
                 :data
                 (dissoc :id)
                 (update :lines (fn [lines] (map #(dissoc % :id) lines)))))))))

(deftest users-favorites
  (let [user (entity/create* (example-user))
        product (entity/create* example-product)
        variation (entity/create :product.variation {:parent (:db/id product)
                                                     :terms #{}})
        variation-json (entity/serialize variation {:culture [:i18n.culture/keyword :en_US]})]

    (server.ws/call-handler-with-user ::sut/users.favorites.add
                                      {:id (:db/id variation)}
                                      user)

    (is (= #{(:db/id variation)}
           (-> (server.ws/call-handler-with-user ::sut/users.favorites.enumerate
                                                 {}
                                                 user)
               :data)))

    (is (= [variation-json]
           (-> (server.ws/call-handler-with-user ::sut/users.favorites.list
                                                 {}
                                                 user)
               :data)))

    (server.ws/call-handler-with-user ::sut/users.favorites.remove
                                      {:id (:db/id variation)}
                                      user)

    (is (= nil
           (-> (server.ws/call-handler-with-user ::sut/users.favorites.enumerate
                                                 {}
                                                 user)
               :data)))))

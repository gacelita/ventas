(ns ventas.server.api-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.database :as db]
   [ventas.database.seed :as seed]
   [ventas.server.ws :as server.ws]
   [ventas.core]
   [ventas.test-tools :as test-tools]
   [taoensso.timbre :as timbre]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.entities.configuration :as entities.configuration]
   [clojure.set :as set]
   [ventas.search :as search]
   [ventas.stats :as stats]
   [ventas.auth :as auth]))

(use-fixtures :once #(with-redefs [db/db (test-tools/test-conn)]
                       (timbre/with-level
                        :report
                        (seed/seed :minimal? true)
                        (%))))

(defn- create-test-category! [& [kw]]
  (entity/create :category
                 {:keyword (or kw :test-category)
                  :name (entities.i18n/get-i18n-entity {:en_US "Test category"})}))

(deftest categories-get
  (let [category (create-test-category!)]
    (is (= (entity/to-json category {:culture [:i18n.culture/keyword :en_US]})
           (-> (server.ws/call-request-handler {:name :categories.get
                                                :params {:id :test-category}}
                                               {})
               :data)))
    (is (= "Invalid ref: :DOES-NOT-EXIST"
           (-> (server.ws/call-request-handler {:name :categories.get
                                                :params {:id :DOES-NOT-EXIST}}
                                               {})
               :data)))
    (is (= "Category not found"
           (-> (server.ws/call-request-handler {:name :categories.get
                                                :params {:id 1112984712}}
                                               {})
               :data)))))

(deftest categories-list
  (doseq [entity (entity/query :category)]
    (entity/delete (:db/id entity)))
  (let [categories (mapv create-test-category! [:test-1 :test-2])]
    (is (= (->> categories
                (map #(entity/to-json % {:culture [:i18n.culture/keyword :en_US]}))
                (map #(dissoc % :id))
                (sort-by :keyword))
           (->> (server.ws/call-request-handler {:name :categories.list}
                                               {})
                :data
                (map #(dissoc % :id))
                (sort-by :keyword))))))

(deftest configuration-get
  (let [data {:stripe.publishable-key "TEST-KEY"
              :site.title "TEST-TITLE"}]
    (doseq [[k v] data]
      (entities.configuration/set! k v))
    (is (= data
           (-> (server.ws/call-request-handler {:name :configuration.get
                                                :params #{:stripe.publishable-key
                                                          :site.title}}
                                               {})
               :data)))))

(deftest entities-find
  (let [category (create-test-category!)]
    (testing "by eid"
      (is (= (entity/to-json category {:culture [:i18n.culture/keyword :en_US]})
             (-> (server.ws/call-request-handler {:name :entities.find
                                                  :params {:id (:db/id category)}})
                 :data))))
    (testing "by lookup-ref"
      (is (= (entity/to-json category {:culture [:i18n.culture/keyword :en_US]})
             (-> (server.ws/call-request-handler {:name :entities.find
                                                  :params {:id [:category/keyword (:category/keyword category)]}})
                 :data))))
    (testing "by slug"
      (let [slug (entity/to-json (entity/find (:ventas/slug category))
                                 {:culture [:i18n.culture/keyword :en_US]})]
        (println "slug" slug)
        (is (= (entity/to-json category {:culture [:i18n.culture/keyword :en_US]})
               (-> (server.ws/call-request-handler {:name :entities.find
                                                    :params {:id slug}})
                   :data)))))
    (testing "unexistent id"
      (is (= "Unable to find entity: 1"
             (-> (server.ws/call-request-handler {:name :entities.find
                                                  :params {:id 1}})
                 :data))))))

(deftest enums-get
  (is (= (db/enum-values (name :order.status) :eids? true)
         (:data (server.ws/call-request-handler {:name :enums.get
                                                 :params {:type :order.status}})))))

(deftest i18n-cultures-list
  (let [fixtures (->> (ventas.database.entity/fixtures :i18n.culture)
                      (map #(dissoc % :schema/type)))]
    (is (= fixtures
           (->> (server.ws/call-request-handler {:name :i18n.cultures.list})
                :data
                (map #(dissoc % :id))
                (map #(set/rename-keys % {:keyword :i18n.culture/keyword
                                          :name :i18n.culture/name})))))))

(deftest image-sizes-list
  (doseq [entity (entity/query :image-size)]
    (entity/delete (:db/id entity)))
  (let [image-size (entity/create :image-size {:algorithm :image-size.algorithm/always-resize
                                               :entities #{:schema.type/product
                                                           :schema.type/category}
                                               :keyword :test-size
                                               :height 65
                                               :width 40})]
    (is (= {:test-size (-> (entity/to-json image-size {:culture [:i18n.culture/keyword :en_US]})
                           (dissoc :keyword))}
           (:data (server.ws/call-request-handler {:name :image-sizes.list}))))))

(def test-taxonomies
  [{:schema/type :schema.type/product.taxonomy
    :product.taxonomy/keyword :test-term-a
    :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "test-taxonomy-a"})}
   {:schema/type :schema.type/product.taxonomy
    :product.taxonomy/keyword :test-term-b
    :product.taxonomy/name (entities.i18n/get-i18n-entity {:en_US "test-taxonomy-b"})}])

(def test-terms
  [{:schema/type :schema.type/product.term
    :product.term/keyword :test-term-a-1
    :product.term/name (entities.i18n/get-i18n-entity {:en_US "test-term-a-1"})
    :product.term/taxonomy [:product.taxonomy/keyword :test-term-a]}

   {:schema/type :schema.type/product.term
    :product.term/keyword :test-term-a-2
    :product.term/name (entities.i18n/get-i18n-entity {:en_US "test-term-a-2"})
    :product.term/taxonomy [:product.taxonomy/keyword :test-term-a]}

   {:schema/type :schema.type/product.term
    :product.term/keyword :test-term-b-1
    :product.term/name (entities.i18n/get-i18n-entity {:en_US "test-term-b-1"})
    :product.term/taxonomy [:product.taxonomy/keyword :test-term-b]}

   {:schema/type :schema.type/product.term
    :product.term/keyword :test-term-b-2
    :product.term/name (entities.i18n/get-i18n-entity {:en_US "test-term-b-2"})
    :product.term/taxonomy [:product.taxonomy/keyword :test-term-b]}])

(def test-products
  [{:schema/type :schema.type/product
    :product/keyword :server-api-product
    :product/name (entities.i18n/get-i18n-entity {:en_US "Example product"})
    :product/variation-terms #{[:product.term/keyword :test-term-a-1]
                               [:product.term/keyword :test-term-a-2]
                               [:product.term/keyword :test-term-b-1]
                               [:product.term/keyword :test-term-b-2]}
    :product/price {:schema/type :schema.type/amount
                    :amount/currency [:currency/keyword :eur]
                    :amount/value 15.6M}}])

(def test-product-variations
  [{:schema/type :schema.type/product.variation
    :product.variation/parent [:product/keyword :server-api-product]
    :product.variation/default? true
    :product.variation/terms #{[:product.term/keyword :test-term-a-1]
                               [:product.term/keyword :test-term-b-1]}}
   {:schema/type :schema.type/product.variation
    :product.variation/parent [:product/keyword :server-api-product]
    :product.variation/default? false
    :product.variation/terms #{[:product.term/keyword :test-term-a-2]
                               [:product.term/keyword :test-term-b-2]}}])

(deftest products-get
  (doseq [entity (concat test-taxonomies test-terms test-products test-product-variations)]
    (entity/create* entity))
  (testing "terms for default variation"
    (is (= #{(db/normalize-ref [:product.term/keyword :test-term-a-1])
             (db/normalize-ref [:product.term/keyword :test-term-b-1])}
           (->> (server.ws/call-request-handler {:name :products.get
                                                 :params {:id [:product/keyword :server-api-product]}})
                :data
                :variation
                (map :selected)
                (map :id)
                (set)))))
  (testing "terms for non-default variation"
    (is (= #{(db/normalize-ref [:product.term/keyword :test-term-a-2])
             (db/normalize-ref [:product.term/keyword :test-term-b-2])}
           (->> (server.ws/call-request-handler {:name :products.get
                                                 :params {:id [:product/keyword :server-api-product]
                                                          :terms #{[:product.term/keyword :test-term-a-2]
                                                                   [:product.term/keyword :test-term-b-2]}}})
                :data
                :variation
                (map :selected)
                (map :id)
                (set)))))
  (testing "terms for nonexisting variation"
    (is (= #{(db/normalize-ref [:product.term/keyword :test-term-a-2])
             (db/normalize-ref [:product.term/keyword :test-term-b-1])}
           (->> (server.ws/call-request-handler {:name :products.get
                                                 :params {:id [:product/keyword :server-api-product]
                                                          :terms #{[:product.term/keyword :test-term-a-2]
                                                                   [:product.term/keyword :test-term-b-1]}}})
                :data
                :variation
                (map :selected)
                (map :id)
                (set))))))

(deftest products-list
  (testing "works without passing params"
    (is (:success (server.ws/call-request-handler {:name :products.list}
                                                  {})))))

(deftest products-aggregations
  (testing "spec does not fail when not passing params"
    (is (= {:data "Elasticsearch error: "
            :id nil
            :success false
            :type :response}
           (server.ws/call-request-handler {:name :products.aggregations}
                                           {}))))
  (doseq [entity (concat test-taxonomies test-terms test-products test-product-variations)]
    (entity/create* entity))
  (entity/create :category {:name (entities.i18n/get-i18n-entity {:en_US "Test category"})
                            :keyword :test-category})
  (let [params (atom nil)]
    (with-redefs [search/search (fn [& args] (reset! params args))]
      (server.ws/call-request-handler {:name :products.aggregations
                                       :params {:filters {:categories #{:test-category}
                                                          :price {:min 0
                                                                  :max 10}
                                                          :terms #{:test-term-a-2}
                                                          :name "Example"}}})
      (is (= {:_source false
              :from 0
              :query {:bool {:must [{:term {:schema/type ":schema.type/product"}}
                                    {:bool {:should [{:bool {:should [{:term {:product/terms (db/normalize-ref [:product.term/keyword :test-term-a-2])}}
                                                                      {:term {:product/variation-terms (db/normalize-ref [:product.term/keyword :test-term-a-2])}}]}}]}}
                                    {:term {:product/categories (db/normalize-ref [:category/keyword :test-category])}}
                                    {:range {:product/price {:gte 0 :lte 10}}}
                                    {:match {:product/name__en_US "Example"}}]}}
              :size 10}
             (first @params))))))

(deftest users-register
  (server.ws/call-request-handler {:name :users.register
                                   :params {:email "test@test.com"
                                            :password "test"
                                            :name "Test user"}})
  (is (= {:schema/type :schema.type/user
          :user/email "test@test.com"
          :user/first-name "Test"
          :user/last-name "user"}
         (-> (entity/find [:user/email "test@test.com"])
             (dissoc :user/password)
             (dissoc :db/id)))))

(def test-user
  {:user/first-name "Test"
   :user/last-name "User"
   :user/email "test2@test.com"
   :user/status :user.status/active
   :user/password "test"
   :user/culture [:i18n.culture/keyword :en_US]
   :schema/type :schema.type/user})

(deftest users-login
  (let [user (entity/create* test-user)]
    (let [session (atom nil)]
      (testing "unexistent user"
        (is (not (:success (server.ws/call-request-handler {:name :users.login
                                                            :params {:email "doesnotexist@test.com"
                                                                     :password "test"}}
                                                           {:session session}))))
        (is (= nil @session))))
    (let [session (atom nil)]
      (testing "valid credentials"
        (is (= {:token (auth/user->token user)
                :user {:culture 17592186045541
                       :email "test2@test.com"
                       :first-name "Test"
                       :last-name "User"
                       :name "Test User"
                       :status :user.status/active}}
               (-> (server.ws/call-request-handler {:name :users.login
                                                    :params {:email "test2@test.com"
                                                             :password "test"}}
                                                   {:session session})
                   :data
                   (update :user #(dissoc % :id)))))

        (is (= {:user (db/normalize-ref [:user/email "test2@test.com"])}
               @session))))
    (let [session (atom nil)]
      (testing "invalid credentials"
        (is (= "Invalid credentials"
               (:data (server.ws/call-request-handler {:name :users.login
                                                       :params {:email "test2@test.com"
                                                                :password "INVALID"}}
                                                      {:session session}))))
        (is (= nil @session))))))

(comment
 (register-endpoint!
  :users.session
  {:spec {(opt :token) (maybe ::string)}}
  (fn [{:keys [params]} {:keys [session]}]
    (if-let [user (get-user session)]
      {:user (entity/to-json user)}
      (if-let [user (some->> (:token params)
                             auth/token->user)]
        (do
          (set-user session user)
          {:user (entity/to-json user)})
        (let [{:keys [user token]} (create-unregistered-user)]
          (set-user session user)
          {:user (entity/to-json user)
           :token token}))))))

(defn- run-temporary-user-test [token]
  (let [session (atom nil)
        result (:data (server.ws/call-request-handler {:name :users.session
                                                       :params {:token token}}
                                                      {:session session}))]
    (is (= (:token result)
           (auth/user->token (entity/find (get-in result [:user :id])))))
    (is (= :user.status/unregistered
           (get-in result [:user :status])))
    (is (= {:user (get-in result [:user :id])}
           @session))))

(deftest users-session
  (let [user (entity/create* test-user)]
    (testing "user in session"
      (let [session (atom {:user (:db/id user)})]
        (is (= {:user (entity/to-json user)}
               (:data (server.ws/call-request-handler {:name :users.session
                                                       :params {}}
                                                      {:session session}))))
        (is (= {:user (:db/id user)}
               @session))))
    (testing "user not in session but token present"
      (let [session (atom nil)]
        (is (= {:user (entity/to-json user)}
               (:data (server.ws/call-request-handler {:name :users.session
                                                       :params {:token (auth/user->token user)}}
                                                      {:session session}))))
        (is (= {:user (:db/id user)}
               @session))))
    (testing "user not in session, token present but invalid"
      (run-temporary-user-test "WELL THIS DOES NOT LOOK LIKE A TOKEN, DOES IT?"))
    (testing "user not in session, no token"
      (run-temporary-user-test nil))))

(deftest users-logout
  (let [session (atom {:user true})]
    (server.ws/call-request-handler {:name :users.logout}
                                    {:session session})
    (is (not (:user @session)))))

(deftest states-list
  (doseq [entity (entity/query :state)]
    (entity/delete (:db/id entity)))
  (let [state (entity/create :state {:name (entities.i18n/get-i18n-entity {:en_US "Test state"})})]
    (is (= "Test state"
           (->> (server.ws/call-request-handler {:name :states.list})
                :data
                first
                :name)))))

(deftest search
  (let [search-params (atom nil)
        stat-params (atom nil)]
    (with-redefs [search/search (fn [& args] (reset! search-params args))
                  stats/record-search-event! (fn [& args] (reset! stat-params args))]
      (server.ws/call-request-handler {:name :search
                                       :params {:search "Test"}})
      (is (= [{:_source false
               :query {:bool {:should [{:match {:brand/name__en_US "Test"}}
                                       {:match {:category/name__en_US "Test"}}
                                       {:match {:product/name__en_US "Test"}}]}}}]
             @search-params))
      (is (= ["Test"]
             @stat-params)))))

(deftest stats-navigation
  (let [stat-params (atom nil)]
    (with-redefs [stats/record-navigation-event! (fn [& args] (reset! stat-params args))]
      (server.ws/call-request-handler {:name :stats.navigation
                                       :params {:handler :test
                                                :params {:id 1}}})
      (is (= [{:handler :test :params {:id 1} :user nil}]
             @stat-params)))))



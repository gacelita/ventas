(ns ventas.server.api
  (:require
   [buddy.hashers :as hashers]
   [byte-streams :as bytes]
   [clojure.string :as str]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as db.generators]
   [ventas.auth :as auth]
   [ventas.entities.user :as entities.user]
   [ventas.server.pagination :as pagination]
   [ventas.server.ws :as server.ws]
   [ventas.entities.product :as entities.product]
   [ventas.paths :as paths]
   [ventas.search :as search]
   [ventas.entities.file :as entities.file]
   [ventas.common.utils :as common.utils]
   [clojure.spec.alpha :as spec]
   [spec-tools.data-spec :as data-spec :refer [opt maybe]]
   [spec-tools.json-schema :as json-schema]
   [ventas.utils :as utils]))

(defonce available-requests (atom {}))

(defn register-endpoint!
  ([kw f]
   (register-endpoint! kw {:binary? false} f))
  ([kw opts f]
   {:pre [(keyword? kw) (ifn? f) (map? opts)]}
   (let [{:keys [binary? middlewares spec nilable-params?] :or {middlewares []}} opts]
     (swap! available-requests assoc kw opts)
     (cond
       (not binary?)
       (defmethod server.ws/handle-request kw [request state]
         (when (and spec (not (and (not (:params request))
                                   nilable-params?)))
           (utils/check (data-spec/spec kw spec)
                        (:params request)))
         (:response (reduce (fn [acc middleware]
                              (middleware acc))
                            {:request request :state state :response (f request state)}
                            middlewares)))
       binary?
       (defmethod server.ws/handle-binary-request kw [request state]
         (f request state))))))

(defn get-user [session]
  (when session
    (:user @session)))

(defn get-culture [session]
  (let [user (get-user session)]
    (or (:user/culture user)
        (:db/id (db/entity [:i18n.culture/keyword :en_US])))))

(spec/def ::id
  (spec-tools.core/create-spec
   {:spec (spec/or :lookup-ref ::db/lookup-ref
                   :eid number?)
    :description "Either a Datomic lookup ref or an entity ID"}))

(spec/def ::keyword
  (spec-tools.core/create-spec
   {:spec keyword?
    :gen (db.generators/keyword-generator)}))

(register-endpoint!
  :categories.get
  {:spec {:id (spec/or :eid number?
                       :keyword-ref ::keyword)}}
  (fn [{{:keys [id]} :params} {:keys [session]}]
    (let [category (-> (cond
                         (number? id) id
                         (keyword? id) [:category/keyword id])
                       (entity/find))]
      (when-not category
        (throw (Exception. "Category not found")))
      (entity/to-json category {:culture (get-culture session)}))))

(register-endpoint!
  :categories.list
  (fn [_ {:keys [session]}]
    (map #(entity/to-json % {:culture (get-culture session)})
         (entity/query :category))))

(register-endpoint!
  :configuration.get
  {:spec {:keyword ::keyword}}
  (fn [{{:keys [keyword]} :params} {:keys [session]}]
    (if-let [value (first (entity/query :configuration {:keyword keyword}))]
      (entity/to-json value {:culture (get-culture session)})
      (throw (Error. (str "Could not find configuration with keyword: " keyword))))))

(register-endpoint!
  :entities.find
  {:spec {:id ::id}}
  (fn [{{:keys [id]} :params} {:keys [session]}]
    (let [entity (entity/find id)]
      (when-not entity
        (throw (Exception. (str "Unable to find entity: " id))))
      (entity/to-json entity {:culture (get-culture session)}))))

(register-endpoint!
  :enums.get
  {:spec {:type ::keyword}}
  (fn [{{:keys [type]} :params} _]
    (db/enum-values (name type))))

(register-endpoint!
  :i18n.cultures.list
  (fn [_ _]
    (->> (entity/query :i18n.culture)
         (map (fn [{:i18n.culture/keys [keyword name] :db/keys [id]}]
                {:keyword keyword
                 :name name
                 :id id})))))

(register-endpoint!
 :image-sizes.list
 (fn [_ {:keys [session]}]
   (->> (entity/query :image-size)
        (map #(entity/to-json % {:culture (get-culture session)}))
        (common.utils/index-by :keyword))))

(register-endpoint!
  :products.get
  {:spec {:id (spec/or :eid number?
                       :keyword-ref ::keyword)
          (opt :terms) (maybe [::id])}}
  (fn [{{:keys [id terms]} :params} {:keys [session]}]
    (-> (cond
          (number? id) id
          (keyword? id) [:product/keyword id])
        (entities.product/find-variation terms)
        (entity/to-json {:culture (get-culture session)}))))

(register-endpoint!
  :products.list
  {:middlewares [pagination/wrap-sort
                 pagination/wrap-paginate]
   :spec {(opt :filters) {(opt :terms) [::id]
                          (opt :price) {(opt :min) number?
                                        (opt :max) number?}}}
   ;; workaround for https://github.com/metosin/spec-tools/issues/100
   :nilable-params? true}
  (fn [{{{:keys [terms price]} :filters} :params} {:keys [session]}]
    (let [wheres
          (as-> (entity/filters->wheres :product {:terms (set terms)}) wheres
                (if (seq price)
                  (let [{:keys [min max] :or {min '?price max '?price}} price]
                    (concat wheres [['?id :product/price '?price]
                                    [[<= min '?price max]]]))
                  wheres))]
      (map #(entity/find-json (:id %) {:culture (get-culture session)})
           (db/nice-query {:find '[?id]
                           :where wheres})))))

(defn- term-counts []
  (->> (db/q '[:find (count ?product-eid)
               ?term-eid
               ?term-translation-value
               ?term-taxonomy
               ?tax-translation-value
               ?tax-keyword
               :where
               [?product-eid :product/terms ?term-eid]
               [?term-eid :product.term/name ?term-name]
               [?term-eid :product.term/taxonomy ?term-taxonomy]
               [?term-name :i18n/translations ?term-translation]
               [?term-translation :i18n.translation/value ?term-translation-value]
               [?term-translation :i18n.translation/culture [:i18n.culture/keyword :en_US]]
               [?term-taxonomy :product.taxonomy/name ?tax-name]
               [?term-taxonomy :product.taxonomy/keyword ?tax-keyword]
               [?tax-name :i18n/translations ?tax-translation]
               [?tax-translation :i18n.translation/value ?tax-translation-value]
               [?tax-translation :i18n.translation/culture [:i18n.culture/keyword :en_US]]])
       (map (fn [[count term-id term-name tax-id tax-name tax-keyword]]
              {:count count
               :id term-id
               :name term-name
               :taxonomy {:id tax-id
                          :name tax-name
                          :keyword tax-keyword}}))))

(defn- prices []
  (let [[min max]
        (->> (db/q '[:find (min ?price) (max ?price)
                     :where [_ :product/price ?price]])
             (first))]
    {:min min :max max}))

(defn- resolve-ref
  "A ref is an identifier that refers to an entity.
   The possibilities are:
     - A keyword, like :category/keyword
     - A slug
     - An eid"
  [ref kw-ident]
  {:pre [(or (number? ref) (string? ref) (keyword? ref))
         (keyword? kw-ident)]}
  (cond
    (number? ref) ref
    (keyword? ref) (:db/id (db/entity [kw-ident ref]))
    (string? ref) (entity/resolve-by-slug ref)))

(defn- get-product-category-filter [categories]
  (mapcat (fn [category-ref]
            (let [category (resolve-ref category-ref :category/keyword)]
              [{:term {:product/categories category}}]))
          categories))

(defn- get-products-query [{:keys [terms categories name price]} culture-kw]
  {:pre [culture-kw]}
  (concat [{:term {:schema/type ":schema.type/product"}}]
          (when terms
            [{:bool {:should (mapcat (fn [term]
                                       [{:bool {:should [{:term {:product/terms term}}
                                                         {:term {:product/variation-terms term}}]}}])
                                     terms)}}])
          (get-product-category-filter categories)
          (when price
            [{:range {:product/price {:gte (:min price)
                                      :lte (:max price)}}}])
          (when name
            [{:match {(search/i18n-field :product/name culture-kw) name}}])))

(defn- term-aggregation->json [{:keys [doc_count buckets]} & [json-opts taxonomy-kw]]
  (entities.product/terms-to-json
   (for [{:keys [key doc_count]} buckets]
     (let [{:keys [taxonomy] :as term} (entity/find-json key json-opts)]
       (merge (dissoc term :keyword)
              {:count doc_count
               :taxonomy (or taxonomy
                             {:id taxonomy-kw
                              :keyword taxonomy-kw})})))))

(defn- aggregate-products [categories culture]
  (let [json-opts {:culture culture}
        aggs-result (search/search {:size 0
                                    :query {:bool {:must (get-product-category-filter categories)}}
                                    :aggs {:categories {:terms {:field "product/categories"}}
                                           :terms {:terms {:field "product/terms"}}
                                           :variation-terms {:terms {:field "product/variation-terms"}}
                                           :brands {:terms {:field "product/brand"}}}})
        aggs (get-in aggs-result [:body :aggregations])]
    (concat (term-aggregation->json (:categories aggs) json-opts :category)
            (term-aggregation->json (:terms aggs) json-opts)
            (term-aggregation->json (:variation-terms aggs) json-opts))))

(defn sorting-field->es [field]
  (get {:price "product/price"}
       field))

(defn- search-products [filters {:keys [items-per-page page sorting]} culture]
  (let [culture-kw (-> culture
                       entity/find
                       :i18n.culture/keyword)
        items-per-page (or items-per-page 10)
        page (or page 0)
        results (search/search
                 (merge
                  {:_source false
                   :query {:bool {:must (get-products-query filters
                                                            culture-kw)}}
                   :size items-per-page
                   :from (* page items-per-page)}
                  (let [{:keys [field direction]} sorting]
                    (when (and field direction)
                      {:sort [{(sorting-field->es field)
                               (name direction)}
                              "_score"]}))))]
    {:can-load-more? (<= items-per-page (get-in results [:body :hits :total]))
     :items (->> (get-in results
                         [:body :hits :hits])
                 (map :_id)
                 (map (fn [v] (Long/parseLong v)))
                 (map #(entity/find-json % {:culture culture})))}))

(register-endpoint!
  :products.aggregations
  {:spec
   {(opt :filters) {(opt :categories) [(spec/or :id ::id
                                                :slug string?)]
                    (opt :price) {(opt :min) number?
                                  (opt :max) number?}
                    (opt :terms) [::id]
                    (opt :name) (maybe string?)}
    (opt :pagination) ::pagination/pagination}
   :nilable-params? true
   :doc
   "Returns:
     - Products filtered by category, price, terms and name
     - Aggregated product terms and categories for the given category"}
  (fn [{{:keys [filters pagination]} :params} {:keys [session]}]
    (let [culture (get-culture session)
          {:keys [items can-load-more?]} (search-products filters pagination culture)]
      {:items items
       :can-load-more? can-load-more?
       :taxonomies (aggregate-products (:categories filters) culture)})))

(register-endpoint!
  :states.list
  {:middlewares [pagination/wrap-paginate]}
  (fn [_ {:keys [session]}]
    (map #(entity/to-json % {:culture (get-culture session)})
         (entity/query :state))))

(register-endpoint!
  :users.login
  {:spec {:email string?
          :password string?}}
  (fn [{{:keys [email password]} :params} {:keys [session]}]
    (when-not (and email password)
      (throw (Exception. "Email and password are required")))
    (let [user (first (entity/query :user {:email email}))]
      (when-not user
        (throw (Exception. "User not found")))
      (when-not (hashers/check password (:user/password user))
        (throw (Exception. "Invalid credentials")))
      (let [token (auth/user->token user)]
        (swap! session assoc :token token)
        {:user (entity/to-json user)
         :token token}))))

(register-endpoint!
  :users.session
  {:spec {(opt :token) (maybe string?)}}
  (fn [{:keys [params]} {:keys [session]}]
    (when-let [token (or (:token params) (get @session :token))]
      (when-let [user (auth/token->user token)]
        (when (:token params)
          (swap! session assoc :token token)
          (swap! session assoc :user user))
        {:identity (entity/to-json user)}))))

(register-endpoint!
  :users.logout
  (fn [_ {:keys [session]}]
    (swap! session dissoc :token :user)
    true))

(register-endpoint!
  :users.register
  {:spec {:email string?
          :password string?
          :name string?}}
  (fn [{{:keys [email password name]} :params} _]
    (when (or (empty? email) (empty? password) (empty? name))
      (throw (Exception. "Email, password and name are required and can't be empty")))
    (let [name-parts (str/split name #" ")
          user (entity/create :user {:first-name (first name-parts)
                                     :last-name (str/join " " (rest name-parts))
                                     :email email
                                     :password password})
          token (auth/user->token user)]
      {:user (entity/to-json user)
       :token token})))

(register-endpoint!
  :search
  {:spec {:search string?}}
  (fn [{{:keys [search]} :params} {:keys [session]}]
    (let [culture (get-culture session)
          {culture-kw :i18n.culture/keyword} (entity/find culture)
          shoulds (for [attr [:product/name
                              :category/name
                              :brand/name]]
                    {:match {(keyword (namespace attr)
                                      (str (name attr) "__" (name culture-kw)))
                             search}})
          hits (-> (search/search {:query {:bool {:should shoulds}}
                                   :_source false})
                   (get-in [:body :hits :hits]))]
      (->> hits
           (map :_id)
           (map (fn [v] (Long/parseLong v)))
           (map #(entity/find-json % {:culture culture
                                      :keep-type? true}))
           (map (fn [{:keys [images] :as result}]
                  (let [result (if images
                                 (assoc result :image (first images))
                                 result)]
                    (select-keys result [:id :type :name :images :image]))))))))

(register-endpoint!
  :upload
  {:binary? true
   :spec {:is-first boolean?
          :is-last boolean?
          (opt :file-id) some?}}
  (fn [{:keys [params]} state]
    (let [{:keys [bytes is-first is-last file-id]} params
          file-id (if is-first (gensym "temp-file") file-id)
          path (str (paths/resolve paths/storage) "/" file-id)]
      (with-open [r (bytes/to-input-stream bytes)
                  w (-> (clojure.java.io/file path)
                        (clojure.java.io/output-stream :append (not is-first)))]
        (clojure.java.io/copy r w))
      (cond
        is-last (entities.file/create-from-file! path)
        is-first file-id
        :default true))))
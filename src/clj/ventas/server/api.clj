(ns ventas.server.api
  (:require
   [buddy.hashers :as hashers]
   [byte-streams :as byte-streams]
   [clojure.spec.alpha :as spec]
   [pantomime.mime :as mime]
   [ring.util.response :refer [response redirect]]
   [taoensso.timbre :as timbre :refer [trace debug info warn error]]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.paths :as paths]
   [ventas.utils :as utils]
   [ventas.utils.images :as utils.images]
   [ventas.auth :as auth]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.server.pagination :as pagination]
   [ventas.server.ws :as server.ws]))

(defn register-endpoint!
  ([kw f]
   (register-endpoint! kw {:binary false} f))
  ([kw opts f]
   {:pre [(keyword? kw) (ifn? f) (map? opts)]}
   (let [{:keys [binary? middlewares] :or {middlewares []}} opts]
     (cond
       (not binary?)
       (defmethod server.ws/handle-request kw [request state]
         (:response (reduce (fn [acc middleware]
                              (middleware acc))
                            {:request request :state state :response (f request state)}
                            middlewares)))
       binary?
       (defmethod server.ws/handle-binary-request kw [request state]
         (f request state))))))

(defn get-culture [session]
  (let [user (:user @session)]
    (or (:user/culture user)
        [:i18n.culture/keyword :en_US])))

(register-endpoint!
  :entities.remove
  (fn [{:keys [params]} state]
    (entity/delete (:id params))))

(register-endpoint!
  :entities.find
  (fn [{:keys [params]} {:keys [session]}]
    (-> (:id params)
        (Long/valueOf)
        (entity/find)
        (entity/to-json {:culture (get-culture session)}))))

(register-endpoint!
  :reference
  (fn [{:keys [params]} _]
    (db/enum-values (name (:type params)))))

(register-endpoint!
  :users.list
  (fn [{{:keys [pagination]} :params} state]
    (let [{items :schema/_type} (db/pull (quote [{:schema/_type [:user/name :db/id :user/email]}])
                                         :schema.type/user)]
      (pagination/paginate (map utils/dequalify-keywords items) pagination))))

(register-endpoint!
  :users.save
  (fn [message state]
    (entity/upsert :user (:params message))))

(register-endpoint!
  :users.login
  (fn [{:keys [params] :as message} {:keys [session]}]
    (let [{:keys [email password]} params]
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
           :token token})))))

(register-endpoint!
  :states.list
  {:middlewares [pagination/wrap-paginate]}
  (fn [_ {:keys [session]}]
    (map #(entity/to-json % {:culture (get-culture session)})
         (entity/query :state))))

(register-endpoint!
  :users.session
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (when-let [token (or (:token params) (get @session :token))]
      (when-let [user (auth/token->user token)]
        (when (:token params)
          (swap! session assoc :token token)
          (swap! session assoc :user user))
        {:identity (entity/to-json user)}))))

(register-endpoint!
  :users.addresses
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [user (:user @session)]
      (->> (entity/query :address {:user (:db/id user)})
           (map #(entity/to-json % {:culture (get-culture session)}))))))

(register-endpoint!
  :users.addresses.save
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [user (:user @session)
          address (entity/find (:id params))]
      (when-not (= (:db/id user) (:address/user address))
        (throw (Exception. "Unauthorized")))
      (entity/upsert :address params))))

(register-endpoint!
  :users.logout
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [token (:token session)]
      (when token
        (swap! session dissoc :token)))))

(register-endpoint!
  :users.register
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [{:keys [email password name]} params]
      (when (or (empty? email) (empty? password) (empty? name))
        (throw (Exception. "Email, password and name are required")))
      (let [user (entity/create :user {:name name
                                       :email email
                                       :password password})
            token (auth/user->token user)]
        {:user (entity/to-json user)
         :token token}))))

(register-endpoint!
  :users.draft-order
  (fn [{:keys [params]} {:keys [session]}]
    (let [user {}]
      (-> (entity/query :order {:status :order.status/draft
                                :user (:id user)})
          (first)
          (entity/find-json {:culture (get-culture session)})))))

(register-endpoint!
  :configuration.get
  (fn [{:keys [params]} {:keys [session]}]
    {:pre [(keyword? (:keyword params))]}
    (let [kw (:keyword params)]
      (if-let [value (first (entity/query :configuration {:keyword kw}))]
        (entity/to-json value {:culture (get-culture session)})
        (throw (Error. (str "Could not find configuration with keyword: " kw)))))))

(register-endpoint!
  :events.list
  (fn [{:keys [pagination params]} {:keys [session]}]
    (let [items (map #(entity/to-json % {:culture (get-culture session)})
                     (entity/query :event))])))

(register-endpoint!
  :products.get
  (fn [{{:keys [id]} :params} {:keys [session]}]
    (-> (cond
          (number? id) id
          (keyword? id) [:product/keyword id])
        (entity/find-json {:culture (get-culture session)}))))

(register-endpoint!
  :products.list
  (fn [{{:keys [pagination filters]} :params} {:keys [session]}]
    (let [{:keys [terms price]} filters]
      (assert (or (nil? terms) (set? terms)))

      (let [wheres
            (as-> (entity/filters->wheres :product {:terms terms}) wheres
                  (if (seq price)
                    (let [{:keys [min max] :or {min '?price max '?price}} price]
                      (concat wheres [['?id :product/price '?price]
                                      [[<= min '?price max]]]))
                    wheres))
            items (map #(entity/find-json (:id %) {:culture (get-culture session)})
                       (db/nice-query {:find '[?id]
                                       :where wheres}))]
        (pagination/paginate items pagination)))))

(register-endpoint!
  :products.save
  (fn [message state]
    (entity/upsert :product (-> (:params message)
                                (update :price bigdec)))))

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
               [?term-translation :i18n.translation/language [:i18n.culture/keyword :en_US]]
               [?term-taxonomy :product.taxonomy/name ?tax-name]
               [?term-taxonomy :product.taxonomy/keyword ?tax-keyword]
               [?tax-name :i18n/translations ?tax-translation]
               [?tax-translation :i18n.translation/value ?tax-translation-value]
               [?tax-translation :i18n.translation/language [:i18n.culture/keyword :en_US]]])
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

(register-endpoint!
  :products.aggregations
  (fn [message _]
    {:taxonomies
     (map (fn [[k v]]
            (assoc k :terms (map #(dissoc % :taxonomy) v)))
          (group-by :taxonomy (term-counts)))
     :prices (prices)}))



(register-endpoint!
  :categories.list
  (fn [{{:keys [pagination]} :params} {:keys [session]}]
    (let [items (map #(entity/to-json % {:culture (get-culture session)})
                     (entity/query :category))]
      (pagination/paginate items pagination))))



(register-endpoint!
  :brands.list
  (fn [{{:keys [pagination]} :params} {:keys [session]}]
    (let [items (map #(entity/to-json % {:culture (get-culture session)})
                     (entity/query :brand))]
      (pagination/paginate items pagination))))



(register-endpoint!
  :taxes.list
  (fn [{{:keys [pagination]} :params} {:keys [session]}]
    (let [items (map #(entity/to-json % {:culture (get-culture session)})
                     (entity/query :tax))]
      (pagination/paginate items pagination))))

(register-endpoint!
  :image-sizes.list
  (fn [{{:keys [pagination]} :params} {:keys [session]}]
    (let [items (map #(entity/to-json % {:culture (get-culture session)})
                     (entity/query :image-size))]
      (pagination/paginate items pagination))))

(register-endpoint!
  :taxes.save
  (fn [message state]
    (entity/upsert :tax (-> (:params message)
                            (update :amount float)))))


(register-endpoint!
  :datadmin/datoms
  (fn [message state]
    (let [datoms (db/datoms :eavt)]
      {:datoms (map db/datom->map (take 10 datoms))})))

(defn save-image [source-path]
  (let [mime (mime/mime-type-of (clojure.java.io/file source-path))
        extension (subs (mime/extension-for-name mime) 1)
        entity (entity/create :file {:extension extension})
        target-path (str paths/images "/" (:db/id entity) "." extension)]
    (.renameTo
     (clojure.java.io/file source-path)
     (clojure.java.io/file target-path))
    (utils.images/transform-image
     target-path
     paths/transformed-images
     {:resize {:width 150}})
    entity))

(register-endpoint!
  :upload
  {:binary? true}
  (fn [{:keys [params]} state]
    (let [{:keys [bytes is-first is-last file-id]} params
          file-id (if is-first (gensym "temp-file") file-id)
          path (str paths/project-resources "/" file-id)]
      (with-open [r (byte-streams/to-input-stream bytes)
                  w (clojure.java.io/output-stream (clojure.java.io/file path) :append (not is-first))]
        (clojure.java.io/copy r w))
      (cond
        is-last (save-image path)
        is-first file-id
        :default true))))

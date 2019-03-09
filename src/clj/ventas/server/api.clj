(ns ventas.server.api
  "Public websocket endpoints"
  (:require
   [buddy.hashers :as hashers]
   [byte-streams :as bytes]
   [clojure.core.async :as core.async]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [slingshot.slingshot :refer [throw+]]
   [spec-tools.data-spec :as data-spec :refer [maybe opt]]
   [ventas.auth :as auth]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as db.generators]
   [ventas.entities.category :as entities.category]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.entities.file :as entities.file]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.entities.product :as entities.product]
   [ventas.entities.user :as entities.user]
   [ventas.i18n :refer [i18n]]
   [ventas.paths :as paths]
   [ventas.search.entities :as search.entities]
   [ventas.search.products :as search.products]
   [ventas.server.pagination :as pagination]
   [ventas.server.ws :as server.ws]
   [ventas.site :as site]
   [ventas.utils :as utils]
   [ventas.utils.slugs :as utils.slugs]))

(defonce available-requests (atom {}))

(defn get-user-id [session]
  {:pre [(or (not session) (utils/atom? session))]}
  (when session
    (:user @session)))

(defn get-user [session]
  (some-> (get-user-id session) entity/find))

(defn set-user [session user]
  (swap! session assoc :user (:db/id user)))

(defn get-culture
  "Returns the eid of the session culture"
  [session]
  (entities.user/get-culture (get-user session)))

(defn serialize-with-session [session entity]
  (entity/serialize entity {:culture (get-culture session)}))

(defn find-serialize-with-session [session eid]
  (entity/find-serialize eid {:culture (get-culture session)}))

(defn register-endpoint!
  ([kw f]
   (register-endpoint! kw {:binary? false} f))
  ([kw opts f]
   {:pre [(keyword? kw) (namespace kw) (ifn? f) (map? opts)]}
   (let [{:keys [binary? middlewares spec] :or {middlewares []}} opts]
     (swap! available-requests assoc kw opts)
     (cond
       (not binary?)
       (defmethod server.ws/handle-request kw [request state]
         (when spec
           (utils/check (data-spec/spec (keyword "api" (name kw)) spec)
                        (:params request)))
         (try
           (:response (reduce (fn [acc middleware]
                                (middleware acc))
                              {:request request :state state :response (f request state)}
                              middlewares))
           (catch Throwable e
             (let [{:keys [type] :as data} (ex-data e)
                   culture-kw (utils/swallow
                               (entities.i18n/culture->kw (get-culture (:session state))))]
               (if-let [localized-message (when type
                                            (i18n (or culture-kw :en_US) type data))]
                 (throw+ (assoc data :message localized-message))
                 (throw e))))))
       binary?
       (defmethod server.ws/handle-binary-request kw [request state]
         (f request state))))))

(spec/def ::keyword
  (spec-tools.core/create-spec
   {:spec keyword?
    :gen db.generators/keyword-generator}))

(spec/def  ::string
  (spec-tools.core/create-spec
   {:spec (spec/and string? (comp not str/blank?))
    :gen db.generators/string-generator}))

(spec/def ::ref
  (spec-tools.core/create-spec
   {:spec (spec/or :eid number?
                   :lookup-ref ::db/lookup-ref
                   :keyword-id ::keyword
                   :slug ::string)
    :description "A ref is an identifier that refers to an entity.
                  The possibilities are:
                   - A keyword, like :test-category
                   - A lookup ref
                   - A slug
                   - An eid"}))

(defn resolve-ref [ref & [kw-ident]]
  {:pre [(utils/check ::ref ref)
         (or (not kw-ident) (keyword? kw-ident))]}
  (let [result (cond
                 (number? ref) ref
                 (keyword? ref) (when kw-ident (:db/id (db/entity [kw-ident ref])))
                 (string? ref) (utils.slugs/resolve-by-slug ref)
                 (db/lookup-ref? ref) (:db/id (db/entity ref)))]
    (when-not result
      (throw+ {:type ::invalid-ref
               :ref ref}))
    result))

(register-endpoint!
 ::categories.get
 {:spec {:id ::ref}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [category (entity/find (resolve-ref id :category/keyword))]
     (when-not category
       (throw+ {:type ::category-not-found
                :category id}))
     (serialize-with-session session category))))

(register-endpoint!
 ::categories.list
 (fn [_ {:keys [session]}]
   (->> (entity/query :category)
        (map (partial serialize-with-session session)))))

(register-endpoint!
 ::categories.options
  {:doc "Lists categories in a format suitable for a dropdown selector"}
  (fn [_ {:keys [session]}]
    (entities.category/options (get-culture session))))

(register-endpoint!
 ::configuration.get
 {:spec [::keyword]}
 (fn [{ids :params} _]
   (entities.configuration/get ids)))

(register-endpoint!
 ::entities.find
 {:spec {:id ::ref}}
 (fn [{{:keys [id]} :params} {:keys [session]}]
   (let [entity (entity/find (resolve-ref id))]
     (when-not entity
       (throw+ {:type ::entity-not-found
                :entity id}))
     (serialize-with-session session entity))))

(register-endpoint!
 ::enums.get
 {:spec {:type ::keyword}}
 (fn [{{:keys [type]} :params} _]
   (db/enum-values (name type) :eids? true)))

(register-endpoint!
 ::i18n.cultures.list
 (fn [_ _]
   (->> (entity/query :i18n.culture)
        (map (fn [{:i18n.culture/keys [keyword name] :db/keys [id]}]
               {:keyword keyword
                :name name
                :id id})))))

(register-endpoint!
 ::image-sizes.list
 (fn [_ {:keys [session]}]
   (->> (entity/query :image-size)
        (map (partial serialize-with-session session))
        (common.utils/index-by :keyword))))

(register-endpoint!
 ::products.get
 {:spec {:id ::ref
         (opt :terms) (maybe [::ref])}}
 (fn [{{:keys [id terms]} :params} {:keys [session]}]
   (serialize-with-session
    session
    (-> (resolve-ref id :product/keyword)
        (entities.product/find-variation terms)))))

(register-endpoint!
  ::realtime-test
  {:doc "A very simple test for realtime capabilities.
         Will send (inc n) every two seconds."}
  (fn [_ _]
    (let [ch (core.async/chan)]
      (core.async/go-loop [n 0]
        (core.async/<! (core.async/timeout 2000))
        (core.async/>! ch n)
        (recur (inc n)))
      ch)))

(register-endpoint!
 ::products.list
 {:middlewares [pagination/wrap-sort
                pagination/wrap-paginate]}
 (fn [_ {:keys [session]}]
   (->> (entity/query :product)
        (map (partial serialize-with-session session)))))

(register-endpoint!
 ::products.aggregations
 {:spec
  (maybe {(opt :filters) {(opt :categories) [::ref]
                          (opt :price) {(opt :min) number?
                                        (opt :max) number?}
                          (opt :terms) [::ref]
                          (opt :name) (maybe ::string)}
          (opt :pagination) ::pagination/pagination})
  :doc
  "Returns:
     - Products filtered by category, price, terms and name
     - Aggregated product terms and categories for the given category"}
 (fn [{{:keys [filters pagination]} :params} {:keys [session]}]
   (let [culture (get-culture session)
         filters (-> filters
                     (common.utils/update-when-some :categories
                                                    (fn [categories]
                                                      (map #(resolve-ref % :category/keyword)
                                                           categories)))
                     (common.utils/update-when-some :terms
                                                    (fn [terms]
                                                      (map #(resolve-ref % :product.term/keyword)
                                                           terms))))
         {:keys [items can-load-more?]} (search.products/search filters pagination culture)]
     {:items items
      :can-load-more? can-load-more?
      :taxonomies (search.products/aggregate (:categories filters) culture)})))

(register-endpoint!
  ::shipping-methods.list
  (fn [_ {:keys [session]}]
    (->> (entity/query :shipping-method)
         (map (partial serialize-with-session session)))))

(register-endpoint!
  ::payment-methods.list
  (fn [_ {:keys [session]}]
    (->> (entity/query :payment-method)
         (map (partial serialize-with-session session)))))

(register-endpoint!
  ::states.list
  {:middlewares [pagination/wrap-paginate]
   :spec {:country ::ref}}
  (fn [{{:keys [country]} :params} {:keys [session]}]
    (->> (entity/query :state {:country (resolve-ref country :country/keyword)})
         (map (partial serialize-with-session session))
         (map #(dissoc % :country)))))

(register-endpoint!
  ::countries.list
  (fn [_ {:keys [session]}]
    (->> (entity/query :country)
         (map (partial serialize-with-session session)))))

(defn- create-unregistered-user
  "Unauthenticated users can be able to add favorite products and create
   temporary orders (carts) by being temporary users.
   Later on, they can make a proper user simply updating the fields of this
   temporary user."
  []
  (let [user (entity/create :user {:status :user.status/unregistered})
        token (auth/user->token user)]
    {:user user
     :token token}))

(register-endpoint!
 ::users.register
 {:spec {:email ::string
         :password ::string
         :name ::string}}
 (fn [{{:keys [email password name]} :params} {:keys [session]}]
   (let [name-parts (str/split name #" ")
         user (entity/create :user {:first-name (first name-parts)
                                    :last-name (str/join " " (rest name-parts))
                                    :email email
                                    :password password})
         token (auth/user->token user)]
     (set-user session user)
     {:user (entity/serialize user)
      :token token})))

(register-endpoint!
 ::users.login
 {:spec {:email ::string
         :password ::string}}
 (fn [{{:keys [email password]} :params} {:keys [session]}]
   (let [user (entity/query-one :user {:email email})]
     (when-not user
       (throw+ {:type ::user-not-found
                :email email}))
     (when-not (hashers/check password (:user/password user))
       (throw+ {:type ::invalid-credentials
                :email email}))
     (let [token (auth/user->token user)]
       (set-user session user)
       {:user (entity/serialize user)
        :token token}))))

(register-endpoint!
 ::users.session
 {:spec {(opt :token) (maybe ::string)}}
 (fn [{:keys [params]} {:keys [session]}]
   (if-let [user (get-user session)]
     {:user (entity/serialize user)}
     (if-let [user (some->> (:token params)
                            auth/token->user)]
       (do
         (set-user session user)
         {:user (entity/serialize user)})
       (let [{:keys [user token]} (create-unregistered-user)]
         (set-user session user)
         {:user (entity/serialize user)
          :token token})))))

(register-endpoint!
 ::users.logout
 (fn [_ {:keys [session]}]
   (swap! session dissoc :user)
   true))

(register-endpoint!
  ::search
  {:spec {:search ::string}
   :doc "Does a fulltext search for `search` in products, categories and brands"}
  (fn [{{:keys [search]} :params} {:keys [session]}]
    (let [culture (get-culture session)]
      (->> (search.entities/search search
                                   #{:product/name
                                     :category/name
                                     :brand/name}
                                   culture)
           (map (fn [{:keys [images type id name full-name]}]
                  {:image (first images)
                   :id id
                   :type type
                   :name (case type
                           :product name
                           :category full-name)}))))))

(register-endpoint!
 ::site.create
 (fn [{{:keys [email password name]} :params} {:keys [session]}]
   (let [subdomain (utils.slugs/slug name)
         site (entity/create* {:schema/type :schema.type/site
                               :site/subdomain subdomain})
         user (site/with-site
               site
               (fn []
                 (entities.configuration/set! :customization/name name)
                 (let [user (entity/create* {:schema/type :schema.type/user
                                             :user/email email
                                             :user/password password
                                             :user/roles #{:user.role/administrator}})]
                   (set-user session user)
                   user)))]
     {:user (entity/serialize user)
      :token (auth/user->token user)
      :subdomain subdomain})))

(defn- filename->extension [s]
  {:post [(< (count %) 5)]}
  (-> s
      (str/split #"\.")
      (last)))

(register-endpoint!
 ::upload
 {:binary? true
  :spec {:first? boolean?
         :last? boolean?
         (opt :file-id) some?
         (opt :filename) string?}}
 (fn [{{:keys [bytes first? last? file-id filename]} :params} _]
   (let [file-id (if first? (gensym "temp-file") file-id)
         path (io/as-file (str (paths/resolve paths/storage)
                               "/" file-id))]
     (with-open [r (bytes/to-input-stream bytes)
                 w (-> (io/file path)
                       (io/output-stream :append (not first?)))]
       (io/copy r w))
     (cond
       last? (let [entity (entities.file/create-from-file! path (filename->extension filename))]
               (io/delete-file path)
               entity)
       first? file-id
       :default true))))

(register-endpoint!
 ::status
 {:doc "Just a dummy status endpoint"}
 (fn [_ _]
  "ok"))
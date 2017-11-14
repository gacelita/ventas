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
   [ventas.server :as server :refer [ws-request-handler ws-binary-request-handler]]
   [ventas.util :as util]
   [ventas.utils.images :as utils.images]))

(defn register-endpoint!
  ([kw f]
   (register-endpoint! kw {:binary false} f))
  ([kw opts f]
   {:pre [(keyword? kw) (ifn? f) (map? opts)]}
   (let [{:keys [binary?]} opts]
     (cond
       (not binary?)
       (defmethod ws-request-handler kw [message state]
         (f message state))
       binary?
       (defmethod ws-binary-request-handler kw [message state]
         (f message state))))))

(defn limit
  ([coll quantity]
   (limit coll 0 quantity))
  ([coll offset quantity]
   (take quantity (drop offset coll))))

(spec/def ::page number?)
(spec/def ::items-per-page number?)
(spec/def ::pagination
  (spec/keys :req-un [::page ::items-per-page]))

(defn- paginate [coll {:keys [items-per-page page] :as pagination}]
  {:pre [(or (nil? pagination) (util/check ::pagination pagination))]}
  (if pagination
    (limit coll
           (* items-per-page page)
           items-per-page)
    coll))

(register-endpoint!
  :entities.remove
  (fn [{:keys [params]} state]
    (entity/delete (entity/find (:id params)))))

(register-endpoint!
  :entities.find
  (fn [{:keys [params]} state]
    (-> (:id params)
        (Long/valueOf)
        (entity/find)
        (entity/to-json))))

(register-endpoint!
  :reference
  (fn [{:keys [params]} _]
    (db/enum-values (name (:type params)))))

(register-endpoint!
  :users.list
  (fn [{{:keys [pagination]} :params} state]
    (let [{items :schema/_type} (db/pull (quote [{:schema/_type [:user/name :db/id :user/email]}])
                                         :schema.type/user)]
      (paginate (map util/dequalify-keywords items) pagination))))

(register-endpoint!
  :users.save
  (fn [message state]
    (entity/upsert :user (:params message))))

(register-endpoint!
  :users.login
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [{:keys [email password]} params]
      (when-not (and email password)
        (throw (Exception. "Email and password are required")))
      (let [user (first (entity/query :user {:email email}))]
        (when-not user
          (throw (Exception. "User not found")))
        (when-not (hashers/check password (:password user))
          (throw (Exception. "Invalid credentials")))
        (swap! session assoc :identity (:id user))
        {:user (entity/to-json user)
         :token {:email email
                 :password password}}))))

(register-endpoint!
  :users.session
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [{:keys [email password]} (:token params)]
      (first (entity/query :user {:email email})))))

(register-endpoint!
  :users.logout
  (fn [{:keys [params] :as message} {:keys [session] :as state}]
    (let [identity (:identity session)]
      (when identity
        (swap! session dissoc :identity)))))

(register-endpoint!
  :users.register
  (fn [{:keys [params] :as message} state]
    (let [{:keys [email password name]} params]
      (if (and (seq email) (seq password) (seq name))
        (entity/create :user {:name name :email email :password password})
        (throw (Exception. "Email, password and name are required"))))))



(register-endpoint!
  :resources.get
  (fn [{:keys [params]} state]
    {:pre [(keyword? (:keyword params))]}
    (let [kw (:keyword params)]
      (if-let [resource (first (entity/query :resource {:keyword kw}))]
        (entity/to-json resource)
        (throw (Error. (str "Could not find resource with keyword: " kw)))))))



(register-endpoint!
  :configuration.get
  (fn [{:keys [params]} state]
    {:pre [(keyword? (:keyword params))]}
    (let [kw (:keyword params)]
      (if-let [value (first (entity/query :configuration {:keyword kw}))]
        (entity/to-json value)
        (throw (Error. (str "Could not find configuration with keyword: " kw)))))))

#_ "

Eventos:
   - cualquier transacción de la base de datos
   - cualquier evento introducido de manera artificial
¿Deberían los eventos ser entidades separadas de las transacciones, con una transacción
 opcionalmente asociada, creados primariamente por una función en la base de datos?
¿Deberían los eventos ser realmente transacciones?
 De ser así, los eventos artificiales podrían ser las únicas entidades
 Esto es muy probablemente más ligero y seguro"

(register-endpoint!
  :events.list
  (fn [{:keys [pagination params]} state]
    (let [items (map entity/to-json (entity/query :event))])))


(register-endpoint!
  :products/get
  (fn [{:keys [params]} state]
    (entity/to-json (entity/find (:id params)))))

(register-endpoint!
  :products.list
  (fn [{{:keys [pagination]} :params} state]
    (let [items (map entity/to-json (entity/query :product))]
      (paginate items pagination))))

(register-endpoint!
  :products.save
  (fn [message state]
    (entity/upsert :product (-> (:params message)
                                (update :price bigdec)))))

(defn term-counts []
  (->> (db/q '[:find (count ?product-eid) ?term-eid ?term-translation-value ?term-taxonomy ?tax-translation-value
               :where
               [?product-eid :product/terms ?term-eid]
               [?term-eid :product.term/name ?term-name]
               [?term-eid :product.term/taxonomy ?term-taxonomy]
               [?term-name :i18n/translations ?term-translation]
               [?term-translation :i18n.translation/value ?term-translation-value]
               [?term-translation :i18n.translation/language [:i18n.language/keyword :en]]
               [?term-taxonomy :product.taxonomy/name ?tax-name]
               [?tax-name :i18n/translations ?tax-translation]
               [?tax-translation :i18n.translation/value ?tax-translation-value]
               [?tax-translation :i18n.translation/language [:i18n.language/keyword :en]]])
       (map (fn [[count term-id term-name tax-id tax-name]]
              {:count count
               :id term-id
               :name term-name
               :taxonomy {:id tax-id
                          :name tax-name}}))))

(register-endpoint!
  :products/term-counts
  (fn [message _]
    (map (fn [[k v]]
           {:taxonomy k
            :terms (map #(dissoc % :taxonomy) v)})
         (group-by :taxonomy (term-counts)))))



(register-endpoint!
  :categories.list
  (fn [{{:keys [pagination]} :params} state]
    (let [items (map entity/to-json (entity/query :category))]
      (paginate items pagination))))



(register-endpoint!
  :brands.list
  (fn [{{:keys [pagination]} :params} state]
    (let [items (map entity/to-json (entity/query :brand))]
      (paginate items pagination))))



(register-endpoint!
  :taxes.list
  (fn [{{:keys [pagination]} :params} state]
    (let [items (map entity/to-json (entity/query :tax))]
      (paginate items pagination))))

(register-endpoint!
  :image-sizes.list
  (fn [{{:keys [pagination]} :params} state]
    (let [items (map entity/to-json (entity/query :image-size))]
      (paginate items pagination))))

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



(defn mime->keyword [mime]
  (case mime
    "image/jpeg" :file.extension/jpg
    "image/png" :file.extension/png
    "image/gif" :file.extension/gif
    "image/tiff" :file.extension/tiff
    false))

(defn save-image [source-path]
  (let [mime (mime/mime-type-of (clojure.java.io/file source-path))
        entity (entity/create :file {:extension (mime->keyword mime)})
        target-path (str paths/images "/" (:db/id entity) (mime/extension-for-name mime))]
    (.renameTo
     (clojure.java.io/file source-path)
     (clojure.java.io/file target-path))
    (utils.images/transform-image
     target-path
     paths/transformed-images
     {:width 150})
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

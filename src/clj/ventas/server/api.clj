(ns ventas.server.api
  (:require
   [ventas.server :as server :refer [ws-request-handler ws-binary-request-handler]]
   [ventas.database.entity :as entity]
   [ventas.database :as db]
   [ventas.config :refer [config]]
   [ring.util.response :refer [response redirect]]
   [pantomime.mime :as mime]
   [buddy.hashers :as hashers]
   [byte-streams :as byte-streams]
   [clojure.spec.alpha :as spec]
   [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf trace debug info warn error)]
   [ventas.util :as util]))

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



(defmethod ws-request-handler :entities.remove [message state]
  (entity/delete (entity/find (get-in message [:params :id]))))

(defmethod ws-request-handler :entities.find [message state]
  (entity/find (Long/valueOf (get-in message [:params :id]))))



(defmethod ws-request-handler :reference.user.role [message state]
  (db/enum-values "user.role"))



(defmethod ws-request-handler :users.list [{:keys [pagination]} state]
  (let [{items :schema/_type} (db/pull (quote [{:schema/_type [:user/name :db/id :user/email]}])
                                       :schema.type/user)]
    (paginate (map util/dequalify-keywords items) pagination)))

(defmethod ws-request-handler :users.save [message state]
  (entity/upsert :user (:params message)))

(defmethod ws-request-handler :users.login [{:keys [params] :as message} {:keys [session] :as state}]
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
               :password password}})))

(defmethod ws-request-handler :users.session [{:keys [params] :as message} {:keys [session] :as state}]
  (let [{:keys [email password]} (:token params)]
    (first (entity/query :user {:email email}))))

(defmethod ws-request-handler :users.logout [{:keys [params] :as message} {:keys [session] :as state}]
  (let [identity (:identity session)]
    (when identity
      (swap! session dissoc :identity))))

(defmethod ws-request-handler :users.register [{:keys [params] :as message} state]
  (let [{:keys [email password name]} params]
    (if (and (seq email) (seq password) (seq name))
      (entity/create :user {:name name :email email :password password})
      (throw (Exception. "Email, password and name are required")))))



(defmethod ws-request-handler :resources.get [{:keys [params]} state]
  {:pre [(keyword? (:key params))]}
  (let [kw (:key params)]
    (if-let [resource (first (entity/query :resource {:keyword kw}))]
      (entity/to-json resource)
      (throw (Error. (str "Could not find resource with id: " kw))))))



(defmethod ws-request-handler :configuration.get [{:keys [params]} state]
  {:pre [(keyword? (:key params))]}
  (let [kw (:key params)]
    (if-let [value (first (entity/query :configuration {:key kw}))]
      (entity/to-json value)
      (throw (Error. (str "Could not find configuration value: " kw))))))



(defmethod ws-request-handler :products/get [{:keys [params]} state]
  (entity/to-json (entity/find (:id params))))

(defmethod ws-request-handler :products.list [{:keys [pagination]} state]
  (let [items (map entity/to-json (entity/query :product))]
    (paginate items pagination)))



(defmethod ws-request-handler :categories.list [{:keys [pagination]} state]
  (let [items (map entity/to-json (entity/query :category))]
    (paginate items pagination)))



(defmethod ws-request-handler :db.pull [message state]
  (db/pull (get-in message [:params :query])
           (get-in message [:params :id])))

(defmethod ws-request-handler :db.query [message state]
  (db/q (get-in message [:params :query])
        (get-in message [:params :filters])))



(defmethod ws-request-handler :datadmin/datoms [message state]
  (let [datoms (db/datoms :eavt)]
    {:datoms (map db/datom->map (take 10 datoms))}))



(defn mime->keyword [mime]
  (case mime
    "image/jpeg" :image.extension/jpg
    "image/png" :image.extension/png
    "image/gif" :image.extension/gif
    "image/tiff" :image.extension/tiff
    false))

(defmethod ws-binary-request-handler :upload [message state]
  (let [buffer (get-in message [:params :bytes])
        is-first (get-in message [:params :is-first])
        is-last (get-in message [:params :is-last])
        file-id (if is-first (gensym "temp-file") (get-in message [:params :file-id]))
        path (str "resources/" file-id)]
    (with-open [r (byte-streams/to-input-stream buffer)
                w (clojure.java.io/output-stream (clojure.java.io/file path) :append (not is-first))]
      (clojure.java.io/copy r w))
    (cond
      is-last
      (let [mime (mime/mime-type-of (clojure.java.io/file path))
            entity (entity/create :image {:extension (mime->keyword mime)
                                          :source (get-in message [:params :source])})]
        (.renameTo
         (clojure.java.io/file path)
         (clojure.java.io/file (str "resources/public/images/" (:id entity) (mime/extension-for-name mime))))
        entity)
      is-first
      file-id
      :default true)))

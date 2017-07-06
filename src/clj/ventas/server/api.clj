(ns ventas.server.api
  (:require [ventas.server :as server :refer [ws-request-handler ws-binary-request-handler]]
            [ventas.database.entity :as entity]
            [ventas.database :as db]
            [ventas.config :refer [config]]
            [ring.util.response :refer [response redirect]]
            [pantomime.mime :as mime]
            [buddy.hashers :as hashers]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf trace debug info warn error)]))

(defmethod ws-request-handler :entities.remove [message state]
  (entity/delete (entity/find (get-in message [:params :id]))))

(defmethod ws-request-handler :entities.find [message state]
  (entity/find (get-in message [:params :id])))

(defmethod ws-request-handler :app.reference/user.role [message state]
  (map (fn [a] {:text (get a 2) :value (get a 0)}) (db/enum-values "user.role")))

(defmethod ws-request-handler :users.list [message state]
  (let [results (db/pull (quote [{:schema/_type [:user/name :db/id :user/email]}]) :schema.type/user)]
    (map (fn [a]
           {:id (:db/id a)
            :name (:user/name a)
            :email (:user/email a)}) (:schema/_type results))))

(defmethod ws-request-handler :users.save [message state]
  (entity/upsert :user (:params message)))

(defmethod ws-request-handler :users.comments.list [message state]
  (let [results (db/pull (quote [{:comment/_target [:db/id :comment/content {:comment/source [:db/id :user/name]}]}]) (get-in message [:params :id]))]
    (map (fn [a]
           (let [dates (entity/dates (:db/id a))]
             {:content (:comment/content a)
              :source (:comment/source a)
              :id (:db/id a)
              :created-at (:created-at dates)})) (:comment/_target results))))

(defmethod ws-request-handler :users.made-comments.list [message state]
  (let [results (db/pull (quote [{:comment/_source [:db/id :comment/content {:comment/target [:db/id :user/name]}]}]) (get-in message [:params :id]))]
    (map (fn [a]
           (let [dates (entity/dates (:db/id a))]
             {:content (:comment/content a)
              :target (:comment/target a)
              :id (:db/id a)
              :created-at (:created-at dates)})) (:comment/_source results))))

(defmethod ws-request-handler :users.images.list [message state]
  (let [results (db/pull (quote [{:image.tag/_target [{:image.tag/image [:db/id {:image/extension [:db/ident]}]}]}]) (get-in message [:params :id]))]
    (map (fn [b]
           (let [a (:image.tag/image b)]
             {:id (:db/id a)
              :url (str (:base-url config) "images/" (:db/id a) "." (name (get-in a [:image/extension :db/ident])))}
             )) (:image.tag/_target results))))

(defmethod ws-request-handler :users.own-images.list [message state]
  (let [results (db/pull (quote [{:image/_source [:db/id {:image/extension [:db/ident]}]}]) (get-in message [:params :id]))]
    (map (fn [a]
           {:id (:db/id a)
            :url (str (:base-url config) "images/" (:db/id a) "." (name (get-in a [:image/extension :db/ident])))}) (:image/_source results))))

(defmethod ws-request-handler :users.friends.list [message state]
  (let [results (db/pull (quote [{:friendship/_target [{:friendship/source [:db/id :user/email :user/name]}]
                                  :friendship/_source [{:friendship/target [:db/id :user/email :user/name]}]}]) (get-in message [:params :id]))]
    (map (fn [a]
           {:id (:db/id a)
            :name (:user/name a)
            :email (:user/email a)}) (concat (map :friendship/target (:friendship/_source results))
                                             (map :friendship/source (:friendship/_target results))))))

(defmethod ws-request-handler :resources/find [message state]
  (entity/query :resource {:keyword (get-in message [:params :id])}))

(defmethod ws-request-handler :datadmin/datoms [message state]
  (let [datoms (db/datoms :eavt)]
    {:datoms (map db/datom->map (take 10 datoms))}))

(defmethod ws-request-handler :resource/get [message state]
  {:pre [(keyword? (get-in message [:params :keyword]))]}
  (let [kw (get-in message [:params :keyword])]
    (if-let [resource (first (entity/query :resource {:keyword kw}))]
      (entity/json resource)
      (throw (Error. (str "Could not find resource with id: " kw))))))

(defmethod ws-request-handler :configuration/get [message state]
  {:pre [(keyword? (get-in message [:params :key]))]}
  (let [kw (get-in message [:params :key])]
    (if-let [value (first (entity/query :configuration {:key kw}))]
      (entity/json value)
      (throw (Error. (str "Could not find configuration value: " kw))))))

(defmethod ws-request-handler :products/get [message state]
  (entity/json (entity/find (read-string (get-in message [:params :id])))))

(defmethod ws-request-handler :products/list [message state]
  (map entity/json (entity/query :product)))

(defmethod ws-request-handler :categories/list [message state]
  (map entity/json (entity/query :category)))

(defmethod ws-request-handler :db.pull [message state]
  (db/pull (get-in message [:params :query])
           (get-in message [:params :id])))

(defmethod ws-request-handler :db.query [message state]
  (db/q (get-in message [:params :query])
        (get-in message [:params :filters])))

(defmethod ws-request-handler :comments.save [message state]
  (entity/upsert :comment (:params message)))

(defmethod ws-request-handler :users/login [{:keys [params] :as message} {:keys [session] :as state}]
  (let [user (entity/query :user {:email (:email params)})]
    (if (hashers/check (:password params) (:password user))
      (swap! session assoc :identity (:id user))
      (throw (Exception. "Invalid credentials")))))

(defmethod ws-request-handler :users/logout [{:keys [params] :as message} {:keys [session] :as state}]
  (let [identity (:identity session)]
    (if identity
      (swap! session dissoc :identity)
      (throw (Exception. "Not logged in")))))

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

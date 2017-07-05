(ns ventas.server
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [mount.core :as mount :refer [defstate]]

            ;; Ring
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :refer [response redirect]]
            [prone.middleware :as prone]

            ;; Auth
            [ventas.database :as db]
            [ventas.database.entity :as entity]
            [datomic.api :as d]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]

            [chord.http-kit :refer [wrap-websocket-handler]]
            [chord.format.fressian :as chord-fressian]
            [clojure.data.fressian :as fressian]
            [clj-uuid :as uuid]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf trace debug info warn error)]
            [clojure.core.async :as a :refer [<! >! put! close! go go-loop]]

            [byte-streams :as byte-streams]
            [pantomime.mime :refer [mime-type-of]]

            [org.httpkit.server :as http-kit]
            [ventas.config :refer [config]]
            [clojure.tools.logging :as log]
            [ventas.util :as util :refer [print-info]]
            [ventas.common.util :as cutil])
  (:gen-class))

(cheshire.generate/add-encoder clojure.lang.Keyword cheshire.generate/encode-str)

;;
;; Auth routes
;;
(defn register-user-route [{session :session
                            params :params
                            :as req}]
  (try
    (debug "Registering user" params)
    (entity/create :user params)
    (response {:result true})
    (catch Exception e
      (response {:result false :message (.getMessage e)}))))

(defn login-route [{session :session
                    params :params
                    :as req}]
  (debug "Session" session params)
  (let [user (entity/query :user {:email (:email params)})]
    (if (hashers/check (:password params) (:password user))
      ; If the credentials are valid
      (assoc (response {:result true}) :session (assoc session :identity (:id user)))

      ; Otherwise
      (response {:result false}))

    ))

(defn logout-route [{session :session}]
  (assoc (redirect "/") :session (dissoc session :identity)))

;;
;; @todo
;; Estructura de REST API pero comunicación por WebSockets.
;; Hay requests, responses y eventos.
;; Las responses son respuestas a requests, pero una request puede tener varias responses.
;; Los eventos son como responses pero sin estar asociadas a una request.
;; A modo de ejemplo, el cliente quiere obtener un usuario, así que pide esto:
;;   {:type :request :name [:users :get-by-id] :params [:user-id 15768945] :id 8974298}
;; Cuando el servidor tenga los usuarios, los envía con una response, para que el cliente
;; sepa qué significa lo que se está enviando:
;;   {:type :response :id 8974298 :result true :data [...]}
;; O bien:
;;   {:type :response :id 8974298 :result false :error-code 5001 :error-message "Internal server error"}
;; El servidor puede enviar varias respuestas con el tiempo:
;;   {:type :request :name [:users :get] :id 20378234}
;;   {:type :response :id 20378234 :data [users...]}
;;   {:type :response :id 20378234 :data [more users...]}
;; Los eventos son datos que se envían sin necesidad de una request:
;;   {:type :event :name :user-left}
(comment
  {:type :method :name [:users :get-by-id] :params {:userId 1893475}})
;; Los eventos tienen este aspecto:
(comment
  {:type :event :name :user-left :params {}})

(def shared-ws-channel (atom nil))
(def shared-ws-mult (atom nil))

;; Multimétodo para eventos enviados por ws (dispatch on name)
(defmulti ws-event-handler (fn [message client-id ws-channel req] (:name message)))

;; Request handler, which is a multimethod
(defmulti ws-request-handler (fn [message state] (:name message)))

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

;; @todo
;;   Returns null pointer exception on unexistent keyword
(defmethod ws-request-handler :resource/get [message state]
  {:pre [(keyword? (get-in message [:params :keyword]))]}
  (let [kw (get-in message [:params :keyword])]
    (if-let [resource (first (entity/query :resource {:keyword kw}))]
      (entity/json resource)
      (throw (Error. (str "Could not find resource with id: " kw))))))

;; @todo
;;   Returns null pointer exception on unexistent keyword
(defmethod ws-request-handler :configuration/get [message state]
  {:pre [(keyword? (get-in message [:params :key]))]}
  (entity/json (first (entity/query :configuration {:key (get-in message [:params :key])}))))

(defmethod ws-request-handler :products/get [message state]
  (entity/json (entity/find (read-string (get-in message [:params :id])))))

(defmethod ws-request-handler :products/list [message state]
  (map entity/json (entity/query :product)))

(defmethod ws-request-handler :db.pull [message state]
  (db/pull (get-in message [:params :query])
           (get-in message [:params :id])))

(defmethod ws-request-handler :db.query [message state]
  (db/q (get-in message [:params :query])
        (get-in message [:params :filters])))

(defmethod ws-request-handler :comments.save [message state]
  (entity/upsert :comment (:params message)))


(defn call-ws-request-handler
  ([message] (call-ws-request-handler message {}))
  ([message state]
   (try
     (let [response (ws-request-handler message state)]
       {:type :response :id (:id message) :success true :data response})
     (catch Exception e
       {:type :response :id (:id message) :success false :data (.getMessage e)})
     (catch Error e
       {:type :response :id (:id message) :success false :data (.getMessage e)}))))

;; Calls an appropiate function depending on message type
(defn ws-message-handler [message client-id ws-channel req]
  ; Get all of that state into a single map, just in case something needs it
  (let [state {:client-id client-id :ws-channel ws-channel :request req}]
    (case (:type message)
      :event (ws-event-handler message state)
      :request
        (go (a/>! ws-channel (call-ws-request-handler message state)))
      :else (debug "Unhandled message: " message))))


;; Crea un websocket por cliente
;; Gracias a "wrap-websocket-handler", la request tiene un
;; canal async ligado al websocket, usado para la comunicación con el cliente
(defn ws-handler [{:keys [ws-channel] :as req}]
  (let [shared-channel (a/chan)
        client-id (uuid/v4)]

    (a/tap @shared-ws-mult shared-channel)
    (go
      ;; Insertar en el canal común el mensaje de que este cliente se ha unido
      (a/>! @shared-ws-channel {:type :event :name :user-joined :params {:client-id client-id}})
      ;; Loop infinito
      (loop []
        ;; Ejecutar una de las siguientes funciones cuando se reciba un mensaje
        ;; por uno de los siguientes canales (similar a un "switch")
        (a/alt!
          ;; Este es el canal asociado al mult común, por aquí llegan mensajes globales 
          shared-channel
            ([message]
              (if message
                (do
                  ;; Cuando llega un mensaje global, se inserta en el canal local (es decir, se envía a este cliente)
                  (a/>! ws-channel message)
                  (recur))
                (a/close! ws-channel)))
          ;; Este es el canal local, por aquí llegan los mensajes que envía el cliente
          ws-channel
            ([message]
              (if message
                (do
                  (ws-message-handler (cutil/process-input-message (:message message)) client-id ws-channel req)
                  (recur))

                 (do
                   (a/untap @shared-ws-mult shared-channel)
                   ))))))))

(defn pantomime-to-keyword [mime]
  (case mime
    "image/jpeg" :image.extension/jpg
    "image/png" :image.extension/png
    "image/gif" :image.extension/gif
    "image/tiff" :image.extension/tiff
    false))

(defmulti ws-binary-request-handler (fn [message state] (:name message)))

(defmethod ws-binary-request-handler :upload [message state]
  (let [buffer (get-in message [:params :bytes])
        is-first (get-in message [:params :is-first])
        is-last (get-in message [:params :is-last])
        file-id (if is-first (gensym "temp-file") (get-in message [:params :file-id]))
        path (str "resources/" file-id)]
    (println "About to write: " buffer)
    (with-open [r (byte-streams/to-input-stream buffer)
                w (clojure.java.io/output-stream (clojure.java.io/file path) :append (not is-first))]
      (clojure.java.io/copy r w))
    (cond 
      is-last
        (do
          (println "The extension: " (mime-type-of (clojure.java.io/file path)))
          (let [mime (mime-type-of (clojure.java.io/file path))
                entity (entity/create :image {:extension (pantomime-to-keyword mime) :source (get-in message [:params :source])})]
            (.renameTo
              (clojure.java.io/file path)
              (clojure.java.io/file (str "resources/public/images/" (:id entity) (pantomime.mime/extension-for-name mime))))
            entity))
      is-first
        file-id
      :default true)))


(defn ws-binary-message-handler [message client-id ws-channel req]
  ; Get all of that state into a single map, just in case something needs it
  (let [state {:client-id client-id :ws-channel ws-channel :request req}
        response (ws-binary-request-handler message state)]
    (go (a/>! ws-channel {:type :response :id (:id message) :data response}))))

(defn ws-binary-handler [{:keys [ws-channel] :as req}]
  (let [client-id (uuid/v4)]
    (go-loop []
      (a/alt!
        ws-channel
          ([message]
            (debug "Received binary message: " message)
            (when message
              (ws-binary-message-handler (:message message) client-id ws-channel req)
              (recur)))))))

(defn wrap-async-channels
 "Wraps a Ring request with a channel and a mult"
 [handler]
 (fn [req]
   (let [ch (a/chan)
         mult (a/mult ch)]
    (try
      (handler (assoc req :ch ch :mult mult))
      (finally
        ;; TODO: this should be handled properly upon client disconnect
        ;;(a/close! ch)
        )))))


(def backend 
  "The Buddy session backend"
  (session-backend))

(defn wrap-user
  "Wraps a Ring request with the active user, based on
   the database and (:identity req)"
  [handler]
  (fn [{user-id :identity :as req}]
    (if-not (nil? user-id)
      (handler (assoc req :user (entity/find user-id)))
      (handler req))))


(defn wrap-prone
  "If the debug mode is enabled, wraps a Ring request
   with the Prone library"
  [handler]
  (if (:debug config)
    (prone/wrap-exceptions handler {:app-namespaces ["ventas"]})
    handler))

;; All routes
(defroutes routes

           ;; Ruta para crear el websocket de cada cliente
           (GET "/ws" [] (-> #(ws-handler %)
                             (wrap-websocket-handler {:format :json-kw})))
           (GET "/binary-ws" [] (-> #(ws-binary-handler %) ;; todo
                                    (wrap-websocket-handler {:format :fressian})))
           (GET "/*" _
             {:status 200
              :headers {"Content-Type" "text/html; charset=utf-8"}
              :body (io/input-stream (io/resource "public/index.html"))})
           (resources "/"))

;; Ring stack
(def http-handler
  (-> routes
      wrap-prone

      ;; Auth
      ;(wrap-user)
      (wrap-authentication backend)
      (wrap-authorization backend)
      (wrap-session)
      (wrap-params)

      (wrap-defaults site-defaults)
      ;; wrap-with-logger
      wrap-gzip))

(def http-debug-handler (wrap-reload http-handler {:dirs ["src/clj"]}))

;; Server lifecycle
(defn stop-server! [stop-fn]
  (print-info "Stopping server")
  (when (ifn? stop-fn) (stop-fn)))

(defn start-server! [& [port]]
  (print-info "Starting server")
  (let [port (Integer. (or port (:http-port config) 10555))
        ring-handler (var http-handler)]
    (infof "URI: `%s`" (format "http://localhost:%s/" port))
    (http-kit/run-server ring-handler {:port port :join? false})))

(defstate server
  :start
  (do
    (reset! shared-ws-channel (a/chan))
    (reset! shared-ws-mult (a/mult @shared-ws-channel))
    (start-server!))
  :stop
  (do
    (a/close! @shared-ws-channel)
    (stop-server! server)))

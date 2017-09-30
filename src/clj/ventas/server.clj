(ns ventas.server
  (:require
   [clojure.java.io :as io]
   [mount.core :as mount :refer [defstate]]
   [clj-uuid :as uuid]
   [taoensso.timbre :as timbre :refer [trace debug info warn error]]
   [clojure.core.async :as core.async :refer [<! >! close! go go-loop chan]]
   [org.httpkit.server :as http-kit]

   [compojure.core :refer [GET defroutes]]
   [compojure.route]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.util.response :as ring.response]
   [prone.middleware :as prone]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.mime-type :as ring.mime-type]
   [chord.http-kit :refer [wrap-websocket-handler]]
   [chord.format.fressian]

   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.config :as config]
   [ventas.common.util :as common.util]
   [ventas.util :as util]
   [clojure.string :as str])
  (:gen-class)
  (:import (clojure.lang Keyword)))

(cheshire.generate/add-encoder Keyword cheshire.generate/encode-str)

(defn timbre-logger
  ([data]
   (timbre-logger nil data))
  ([opts data]
   (let [{:keys [no-stacktrace?]} opts
         {:keys [level ?err msg_ ?ns-str ?file ?line]} data]
     (str
      (str/upper-case (name level)) " "
      "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
      (force msg_)
      (when-not no-stacktrace?
        (when-let [err ?err]
          (str "\n" (timbre/stacktrace err opts))))))))

(timbre/merge-config!
 {:level :trace
  :output-fn timbre-logger})

(def shared-ws-channel (atom nil))
(def shared-ws-mult (atom nil))

(defmulti ws-event-handler (fn [message client-id ws-channel req] (:name message)))

(defmulti ws-request-handler (fn [message state] (:name message)))

(defmethod ws-request-handler :default [message state]
  (throw (ex-info (str "The " (:name message) " API call is not implemented") {})))

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
(defn ws-message-handler [message client-id session ws-channel req]
  ; Get all of that state into a single map, just in case something needs it
  (let [state {:client-id client-id :ws-channel ws-channel :request req :session session}]
    (case (:type message)
      :event
      (ws-event-handler message state)
      :request
      (let [result (call-ws-request-handler message state)]
        (if (util/chan? result)
          (go-loop []
            (>! ws-channel (<! result))
            (recur))
          (go (>! ws-channel result))))
      :else (debug "Unhandled message: " message))))


;; Crea un websocket por cliente
;; Gracias a "wrap-websocket-handler", la request tiene un
;; canal async ligado al websocket, usado para la comunicación con el cliente
(defn ws-handler [{:keys [ws-channel] :as req}]
  (let [shared-channel (chan)
        client-id (uuid/v4)
        session (atom {})]

    (core.async/tap @shared-ws-mult shared-channel)
    (go
      ;; Insertar en el canal común el mensaje de que este cliente se ha unido
      (>! @shared-ws-channel {:type :event :name :user-joined :params {:client-id client-id}})
      ;; Loop infinito
      (loop []
        ;; Ejecutar una de las siguientes funciones cuando se reciba un mensaje
        ;; por uno de los siguientes canales (similar a un "switch")
        (core.async/alt!
          ;; Este es el canal asociado al mult común, por aquí llegan mensajes globales
          shared-channel
            ([message]
              (if message
                (do
                  ;; Cuando llega un mensaje global, se inserta en el canal local (es decir, se envía a este cliente)
                  (>! ws-channel message)
                  (recur))
                (close! ws-channel)))
          ;; Este es el canal local, por aquí llegan los mensajes que envía el cliente
          ws-channel
            ([message]
              (if message
                (do
                  (ws-message-handler (common.util/process-input-message (:message message)) client-id session ws-channel req)
                  (recur))

                 (do
                   (core.async/untap @shared-ws-mult shared-channel)
                   ))))))))

(defmulti ws-binary-request-handler (fn [message state] (:name message)))

(defn ws-binary-message-handler [message client-id ws-channel req]
  ; Get all of that state into a single map, just in case something needs it
  (let [state {:client-id client-id :ws-channel ws-channel :request req}
        response (ws-binary-request-handler message state)]
    (go (>! ws-channel {:type :response :id (:id message) :data response}))))

(defn ws-binary-handler [{:keys [ws-channel] :as req}]
  (let [client-id (uuid/v4)]
    (go-loop []
      (core.async/alt!
        ws-channel
          ([message]
            (when message
              (debug "Received binary message: " message)
              (ws-binary-message-handler (:message message) client-id ws-channel req)
              (recur)))))))

(defn wrap-async-channels
 "Wraps a Ring request with a channel and a mult"
 [handler]
 (fn [req]
   (let [ch (chan)
         mult (core.async/mult ch)]
    (try
      (handler (assoc req :ch ch :mult mult))
      (finally
        ;; TODO: this should be handled properly upon client disconnect
        ;;(core.async/close! ch)
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
  (if (config/get :debug)
    (prone/wrap-exceptions handler {:app-namespaces ["ventas"]})
    handler))

(defn- add-mime-type [response path]
  (if-let [mime-type (ring.mime-type/ext-mime-type path (:mime-types {}))]
    (ring.response/content-type response mime-type)
    response))

;; All routes
(defroutes routes
  (GET "/ws/json-kw" [] (-> #(ws-handler %)
                    (wrap-websocket-handler {:format :json-kw})))
  (GET "/ws/fressian" [] (-> #(ws-binary-handler %) ;; todo
                           (wrap-websocket-handler {:format :fressian})))
  (GET "/files/*" {{resource-path :*} :route-params}
    (let [root "public"
          resource-response (ring.response/resource-response (str root "/" resource-path))]
      (if resource-response
        (add-mime-type resource-response resource-path)
        (compojure.route/not-found nil))))
  (GET "/*" _
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (io/input-stream (io/resource "public/index.html"))}))

;; Ring stack
(def http-handler
  (-> routes
      (wrap-prone)
      (wrap-authentication backend)
      (wrap-authorization backend)
      (wrap-session)
      (wrap-params)
      (wrap-defaults site-defaults)
      (wrap-gzip)))

(def http-debug-handler (wrap-reload http-handler {:dirs ["src/clj"]}))

;; Server lifecycle
(defn stop-server! [stop-fn]
  (util/print-info "Stopping server")
  (when (ifn? stop-fn) (stop-fn)))

(defn start-server! [& [port]]
  (util/print-info "Starting server")
  (let [port (Integer. (or port (config/get :http-port) 10555))
        ring-handler (var http-handler)]
    (info "Starting server on port:" port)
    (http-kit/run-server ring-handler {:port port :join? false})))

(defstate server
  :start
  (do
    (reset! shared-ws-channel (chan))
    (reset! shared-ws-mult (core.async/mult @shared-ws-channel))
    (start-server!))
  :stop
  (do
    (close! @shared-ws-channel)
    (stop-server! server)))

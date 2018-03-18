(ns ventas.server.ws
  (:require
   [clj-uuid :as uuid]
   [clojure.core.async :as core.async :refer [<! >! chan go go-loop]]
   [clojure.core.async.impl.protocols :as core.async.protocols]
   [taoensso.timbre :as timbre]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]
   [slingshot.slingshot :refer [throw+]]))

(def ^:private shared-hub
  (atom nil))

(def ^:private shared-mult
  (atom nil))

(defmulti handle-event
  (fn [{:keys [name]} _]
    name))

(defmulti handle-request
  (fn [{:keys [name]} _]
    name))

(defmethod handle-request :default [{:keys [name]} _]
  (throw+ {:type ::api-call-not-found
           :name name}))

(defn- error-response [{:keys [id]} e]
  {:type :response
   :id id
   :success false
   :data (or (ex-data e) (.getMessage e) (str e))})

(defn call-request-handler [{:keys [id] :as message} & [state]]
  (try
    (let [response (handle-request message state)]
      {:type :response
       :id id
       :success true
       :data response
       :realtime? (utils/chan? response)})
    (catch Throwable e
      (error-response message e))))

(defn send-message [{:keys [data] :as message} channel]
  (if (utils/chan? data)
    (go-loop []
      (>! channel (assoc message :data (<! data)))
      (when (and (not (core.async.protocols/closed? data))
                 (not (core.async.protocols/closed? channel)))
        (recur)))
    (core.async/put! channel message)))

(defn handle-message [{:keys [type] :as message} {:keys [channel] :as state}]
  (case type
    :event (-> (handle-event message state)
               (send-message channel))
    :request (-> (call-request-handler message state)
                 (send-message channel))
    (timbre/debug "Unhandled message: " message)))

(defn get-shared-channel []
  (let [ch (chan)]
    (core.async/tap @shared-mult ch)
    ch))

(defn close-shared-channel! [channel]
  (core.async/untap @shared-mult channel)
  (core.async/close! channel))

(defn send-shared-message! [message]
  (core.async/put! shared-hub message))

(defmulti handle-messages
  (fn [format _]
    format))

(defmethod handle-messages :transit-json [_ {:keys [ws-channel] :as request}]
  (let [shared-channel (get-shared-channel)
        client-id (uuid/v4)
        session (atom {})
        output-channel (chan)]
    (core.async/pipe shared-channel ws-channel)
    (core.async/pipe output-channel ws-channel)
    (go-loop []
      (if-let [message (<! ws-channel)]
        (do
          (handle-message
           (:message message)
           {:client-id client-id
            :session session
            :channel output-channel
            :request request})
          (recur))
        (core.async/close! output-channel)))))

(defmulti handle-binary-request
  (fn [{:keys [name]} state]
    name))

(defmethod handle-messages :fressian [_ {channel :ws-channel :as request}]
  (let [client-id (uuid/v4)]
    (go-loop []
      (when-let [{{:keys [id] :as message} :message} (<! channel)]
        (timbre/debug "Received binary message" message)
        (let [response (handle-binary-request
                        message
                        {:client-id client-id
                         :channel channel
                         :request request})]
          (>! channel
              {:type :response
               :id id
               :data response}))
        (recur)))))

(defn start! []
  (reset! shared-hub (chan))
  (reset! shared-mult (core.async/mult @shared-hub)))

(defn stop! []
  (some-> @shared-hub core.async/close!))

(defn call-handler-with-user
  "Simulates a request done from the client. Meant for the REPL."
  [name params user]
  {:pre [(entity/entity? user)]}
  (call-request-handler {:name name
                         :params params}
                        {:session (atom {:user (:db/id user)})}))

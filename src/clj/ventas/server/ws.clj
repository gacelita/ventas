(ns ventas.server.ws
  (:require
   [clojure.core.async :as core.async :refer [<! >! go go-loop chan]]
   [ventas.common.utils :as common.utils]
   [ventas.utils :as utils]
   [taoensso.timbre :refer [debug]]
   [clj-uuid :as uuid]
   [ventas.database.entity :as entity]))

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
  (throw
   (ex-info (str "The " name " API call is not implemented") {})))

(defn- error-response [{:keys [id]} e]
  {:type :response
   :id id
   :success false
   :data (or (.getMessage e) (str e))})

(defn call-request-handler [{:keys [id] :as message} state]
  (try
    (let [response (handle-request message state)]
      {:type :response
       :id id
       :success true
       :data response})
    (catch Exception e
      (error-response message e))
    (catch Error e
      (error-response message e))))

(defn send-response [response channel]
  (go (>! channel (common.utils/process-output-message response))))

(defn send-event [response channel]
  (go (>! channel response)))

(defn handle-message [{:keys [type] :as message} {:keys [client-id session channel request] :as state}]
  (case type
    :event (-> (handle-event message state)
               (send-event channel))
    :request (-> (call-request-handler message state)
                 (send-response channel))
    :else (debug "Unhandled message: " message)))

(defn get-shared-channel []
  (let [ch (chan)]
    (core.async/tap @shared-mult ch)
    ch))

(defn close-shared-channel! [channel]
  (core.async/untap @shared-mult channel)
  (core.async/close! channel))

(defn send-shared-message! [message]
  (go (>! shared-hub message)))

(defmulti handle-messages
  (fn [format _]
    format))

(defmethod handle-messages :json [_ {channel :ws-channel :as request}]
  (let [shared-channel (get-shared-channel)
        client-id (uuid/v4)
        session (atom {})]
    (go-loop []
      (if-let [message (<! shared-channel)]
        (do
          (>! channel message)
          (recur))
        (close-shared-channel! shared-channel)))
    (go-loop []
      (if-let [message (<! channel)]
        (do
          (handle-message
           (common.utils/process-input-message (:message message))
           {:client-id client-id
            :session session
            :channel channel
            :request request})
          (recur))
        (core.async/close! channel)))))

(defmulti handle-binary-request
  (fn [{:keys [name]} state]
    name))

(defmethod handle-messages :fressian [_ {channel :ws-channel :as request}]
  (let [client-id (uuid/v4)]
    (go-loop []
      (when-let [{{:keys [id] :as message} :message} (<! channel)]
        (debug "Received binary message" message)
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
                        {:session (atom {:user user})}))
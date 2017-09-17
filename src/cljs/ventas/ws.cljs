(ns ventas.ws
  "Requests and responses, abstracted over websocket communication"
  (:require [ventas.utils.logging :refer [trace debug info warn error]]
            [cljs.core.async :refer [<! >! close! chan]]
            [chord.client :as chord]
            [chord.format.fressian]
            [ventas.common.util :as common.util])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(def ^:private output-json-channel (atom nil))
(def ^:private output-binary-channel (atom nil))
(def ^:private active-request-channels (atom {}))

(defn send-request!
  "Sends a request and calls the callback with the response"
  [{:keys [name params callback]} & {:keys [binary? realtime?]}]
  (debug ::send-request!
         {:name name
          :params params
          :binary? binary?
          :realtime? realtime?})
  (let [request-channel (chan)
        request-id (gensym "request-")
        output-channel (if binary? @output-binary-channel @output-json-channel)]
    (swap! active-request-channels assoc request-id request-channel)
    (go
     (>! output-channel
         {:type :request
          :id request-id
          :name name
          :params params})
     (loop []
       (let [message (<! request-channel)]
         (callback message)
         (if realtime?
           (recur)
           (do (close! request-channel)
               (swap! active-request-channels dissoc request-id))))))))

(defn- ws-response-dispatch
  "Puts a response into its corresponding channel"
  [message]
  (let [channel (get @active-request-channels (:id message))]
    (when channel
      (go (>! channel message)))))

(defmulti ws-event-dispatch
  "Calls an event handler, if any"
  :name)

(declare init)

(defmethod ws-event-dispatch :server-restarted [_]
  (init))

(defmethod ws-event-dispatch :default [event]
  (debug "Unhandled event: " event))

(defn receive-messages! [websocket-channel]
  "Receives messages from the server and calls an appropiate dispatcher"
  (go-loop []
    (let [{:keys [type] :as message} (common.util/process-input-message (:message (<! websocket-channel)))]
      (case type
        :event (ws-event-dispatch message)
        :response (ws-response-dispatch message)
        (warn "Unhandled websocket message" message))
      (if message
        (recur)
        (init)))))

(defn- send-messages!
  "Receives messages from output-channel and send them to the server"
  [output-channel websocket-channel]
  (go-loop []
    (when-let [message (common.util/process-output-message (<! output-channel))]
      (>! websocket-channel message)
      (recur))))

(defn- start-websocket [format]
  {:pre [(#{:fressian :json-kw} format)]}
  (go
   (let [channel (chan)
         {:keys [ws-channel] ws-error :error} (<! (chord/ws-ch "ws://localhost:3450/ws/fressian" {:format format}))]
     (if ws-error
       (do
         (error "Error connecting to the " format " websocket: " ws-error)
         (>! channel false))
       (do
         (reset! output-json-channel
                 (doto (chan)
                   (send-messages! ws-channel)))
         (receive-messages! ws-channel)
         (>! channel true))))))

(defn init
  "Starts the websockets"
  []
  (let [channel (chan)]
    (go
      (info "Starting websockets")
      (let [json-result (<! (start-websocket :json-kw))
            fressian-result (<! (start-websocket :fressian))]
        (>! channel (and json-result fressian-result))))
    channel))
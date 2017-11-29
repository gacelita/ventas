(ns ventas.ws
  "Requests and responses, abstracted over websocket communication"
  (:require [ventas.utils.logging :refer [trace debug info warn error]]
            [cljs.core.async :refer [<! >! close! chan]]
            [chord.client :as chord]
            [chord.format.fressian]
            [ventas.common.utils :as common.utils]
            [ventas.components.notificator :as notificator]
            [re-frame.core :as rf])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defonce ^:private request-channels (atom {}))
(defonce ^:private output-channels (atom {}))
(def ws-upload-chunk-size (* 1024 50))

(defn output-binary-channel []
  (get @output-channels :fressian))

(defn output-json-channel []
  (get @output-channels :json-kw))

(defn send-request!
  "Sends a request and calls the callback with the response"
  [{:keys [params callback] request-name :name} & {:keys [binary? realtime?]}]
  (debug ::send-request!
         {:name request-name
          :params params
          :binary? binary?
          :realtime? realtime?})
  (let [request-channel (chan)
        request-id (str (gensym (str "request-" (name request-name) "-")))
        output-channel (if binary? (output-binary-channel) (output-json-channel))]
    (swap! request-channels assoc request-id request-channel)
    (go
     (>! output-channel
         {:type :request
          :id request-id
          :name request-name
          :params params})
     (loop []
       (let [message (<! request-channel)]
         (callback message)
         (if realtime?
           (recur)
           (do (close! request-channel)
               (swap! request-channels dissoc request-id))))))))

(defn- ws-response-dispatch
  "Puts a response into its corresponding channel"
  [message]
  (let [channel (get @request-channels (:id message))]
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

(defn receive-messages! [websocket-channel format]
  "Receives messages from the server and calls an appropiate dispatcher"
  (go-loop []
    (let [message (:message (<! websocket-channel))
          {:keys [type] :as message} (if (= format :json-kw)
                                       (common.utils/process-input-message message)
                                       message)]
      (debug ::receive-messages! message)
      (case type
        :event (ws-event-dispatch message)
        :response (ws-response-dispatch message)
        (warn "Unhandled websocket message" message))
      (if (seq message)
        (recur)
        (init)))))

(defn- send-messages!
  "Receives messages from output-channel and send them to the server"
  [output-channel websocket-channel format]
  (go-loop []
   (let [message (<! output-channel)
         message (if (= format :json-kw)
                   (common.utils/process-output-message message)
                   message)]
     (>! websocket-channel message)
     (recur))))

(defn- start-websocket [format]
  {:pre [(#{:fressian :json-kw} format)]}
  (let [channel (chan)]
    (go
     (let [url (str "ws://localhost:3450/ws/" (name format))
           {:keys [ws-channel] ws-error :error} (<! (chord/ws-ch url {:format format}))]
       (if ws-error
         (do
           (error "Error connecting to the " format " websocket: " ws-error)
           (>! channel false))
         (do
           (swap! output-channels assoc format (doto (chan)
                                                 (send-messages! ws-channel format)))
           (receive-messages! ws-channel format)
           (>! channel true)))))
    channel))

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

(defn- call [fx-or-fn data]
  (condp #(%1 %2) fx-or-fn
    keyword? (rf/dispatch [fx-or-fn data])
    fn? (fx-or-fn data)
    (error "Not an effect or function: " fx-or-fn)))

(defn effect-ws-request [{:keys [name params success error]}]
  (send-request!
   {:name name
    :params params
    :callback
    (fn [{data :data request-succeeded? :success}]
      (cond
        (not request-succeeded?)
          (do
            (rf/dispatch [::notificator/add {:message data :theme "warning"}])
            (when error
              (call error data)))
        success
        (call success data)))}))


(defn effect-ws-upload-request [request & [file-id start]]
  (let [file-id (or file-id false)
        start (or start 0)
        {:keys [name upload-data params upload-key success]} request
        data-length (-> upload-data .-byteLength)
        raw-end (+ start ws-upload-chunk-size)
        is-last (> raw-end data-length)
        is-first (zero? start)
        end (if is-last data-length raw-end)]
    (send-request!
     {:name name
      :params (merge params
                     {upload-key (.slice upload-data start end)
                      :is-last is-last
                      :is-first is-first
                      :file-id file-id})
      :callback (fn [{:keys [data]}]
                  (if is-last
                    (success data)
                    (effect-ws-upload-request request
                                              (if is-first data file-id)
                                              end)))}
     :binary? true)))

(rf/reg-fx :ws-request effect-ws-request)

(rf/reg-fx
 :ws-request-multi
 (fn [requests]
   (doseq [request requests]
     (effect-ws-request request))))

(rf/reg-event-fx
 :effects/ws-request
 (fn [cofx [_ data]]
   {:ws-request data}))

(rf/reg-fx :ws-upload-request effect-ws-upload-request)

(rf/reg-event-fx
 :effects/ws-upload-request
 (fn [cofx [_ data]]
   {:ws-upload-request data}))
(ns ventas.ws
  "Requests and responses, abstracted over websocket communication"
  (:require
   [ventas.utils.logging :as log]
   [cljs.core.async :refer [<! >! close! chan]]
   [chord.client :as chord]
   [chord.format.fressian]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defonce ^:private pending-requests (reagent/atom #{}))

(rf/reg-sub-raw
 ::pending-requests
 (fn [_ _]
   (ratom/reaction @pending-requests)))

(defonce ^:private request-channels (atom {}))
(defonce ^:private output-channels (atom {}))
(def ws-upload-chunk-size (* 1024 50))

(defn output-binary-channel []
  (get @output-channels :fressian))

(defn output-json-channel []
  (get @output-channels :json))

(defn send-request!
  "Sends a request and calls the callback with the response"
  [{:keys [params callback] request-name :name} & {:keys [binary? realtime?]}]
  (log/debug ::send-request!
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
  (log/debug "Unhandled event: " event))

(defn receive-messages! [websocket-channel format]
  "Receives messages from the server and calls an appropiate dispatcher"
  (go-loop []
    (let [message (:message (<! websocket-channel))
          {:keys [type] :as message} (if (= format :json)
                                       (common.utils/process-input-message message)
                                       message)]
      (log/debug ::receive-messages! message)
      (case type
        :event (ws-event-dispatch message)
        :response (ws-response-dispatch message)
        (log/warn "Unhandled websocket message" message))
      (if (seq message)
        (recur)
        (init)))))

(defn- send-messages!
  "Receives messages from output-channel and send them to the server"
  [output-channel websocket-channel format]
  (go-loop []
   (let [message (<! output-channel)
         message (if (= format :json)
                   (common.utils/process-output-message message)
                   message)]
     (>! websocket-channel message)
     (recur))))

(defn- websocket-url [format]
  (str (if (= "https:" (-> js/document .-location .-protocol))
         "wss://"
         "ws://")
       (-> js/document .-location .-host)
       "/ws/" (name format)))

(defn- start-websocket [format]
  {:pre [(#{:fressian :json} format)]}
  (let [channel (chan)]
    (go
     (let [url (websocket-url format)
           {:keys [ws-channel] ws-error :error} (<! (chord/ws-ch url {:format format}))]
       (if ws-error
         (do
           (log/error "Error connecting to the " format " websocket: " ws-error)
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
      (log/info "Starting websockets")
      (let [json-result (<! (start-websocket :json))
            fressian-result (<! (start-websocket :fressian))]
        (>! channel (and json-result fressian-result))))
    channel))

(defn- call [to-call data]
  (condp #(%1 %2) to-call
    keyword? (rf/dispatch [to-call data])
    vector? (rf/dispatch (conj to-call data))
    fn? (to-call data)
    (log/error "Not an effect or function: " to-call)))

(defn- effect-ws-request [{:keys [name params success error] :as request}]
  (let [args-hash (hash request)]
    (when-not (contains? @pending-requests args-hash)
      (swap! pending-requests conj args-hash)
      (send-request!
       {:name name
        :params params
        :callback
        (fn [{data :data request-succeeded? :success :as response}]
          (swap! pending-requests disj args-hash)
          (cond
            (not request-succeeded?)
            (do
              (rf/dispatch [::notificator/add {:message data :theme "warning"}])
              (log/error "Request failed!" response)
              (when error
                (call error data)))
            success
            (call success data)))}))))


(defn- effect-ws-upload-request
  [{:keys [name upload-data params upload-key success] :as request} & [file-id start]]
  (let [file-id (or file-id false)
        start (or start 0)
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
                    (call success data)
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
(ns ventas.ws
  "Requests and responses, abstracted over websocket communication"
  (:require
   [chord.client :as chord]
   [chord.format.fressian]
   [cljs.core.async :refer [<! >! chan close! timeout]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.server.api :as api]
   [reagent.ratom :as ratom]
   [ventas.events :as events]
   [ventas.utils.logging :as log])
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

(defn send-request!
  "Sends a request and calls the callback with the response"
  [{:keys [params channel-key callback] request-name :name} & {:keys [binary?]}]
  (log/debug ::send-request!
             {:name request-name
              :params params
              :channel-key channel-key
              :binary? binary?})
  (let [request-channel (chan)
        request-id (str (gensym (str "request-" (name request-name) "-")))
        output-channel (get @output-channels (if binary? :fressian :transit-json))]
    (swap! request-channels assoc request-id request-channel)
    (go
      (>! output-channel
          {:type :request
           :id request-id
           :name request-name
           :params params
           :channel-key channel-key})
      (loop []
        (let [{:keys [realtime?] :as message} (<! request-channel)]
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

(declare start-websocket)

(defn restart
  "Restarts a websocket"
  [format]
  (go
   (<! (start-websocket format))
   (when (= :transit-json format)
     (rf/dispatch [::events/users.session]))))

(defmethod ws-event-dispatch :default [event]
  (log/debug "Unhandled event: " event))

(defn receive-messages! [websocket-channel format]
  "Receives messages from the server and calls an appropiate dispatcher"
  (go-loop []
    (let [{:keys [type] :as message} (:message (<! websocket-channel))]
      (log/debug ::receive-messages! message)
      (case type
        :event (ws-event-dispatch message)
        :response (ws-response-dispatch message)
        (log/warn "Unhandled websocket message" message))
      (if (seq message)
        (recur)
        (do
          (<! (timeout 1000))
          (<! (restart format)))))))

(defn- send-messages!
  "Receives messages from output-channel and send them to the server"
  [output-channel websocket-channel]
  (go-loop []
    (let [message (<! output-channel)]
      (>! websocket-channel message)
      (recur))))

(defn- websocket-url [format]
  (str (if (= "https:" (-> js/document .-location .-protocol))
         "wss://"
         "ws://")
       (-> js/document .-location .-host)
       "/ws/" (name format)))

(defn- start-websocket [format]
  {:pre [(#{:fressian :transit-json} format)]}
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
                                                  (send-messages! ws-channel)))
            (receive-messages! ws-channel format)
            (>! channel true)))))
    channel))

(defn init
  "Starts the websockets"
  []
  (let [channel (chan)]
    (go
      (log/info "Starting websockets")
      (let [transit-result (<! (start-websocket :transit-json))
            fressian-result (<! (start-websocket :fressian))]
        (>! channel (and transit-result fressian-result))))
    channel))

(defn- call [to-call data]
  (condp #(%1 %2) to-call
    keyword? (rf/dispatch [to-call data])
    vector? (rf/dispatch (conj to-call data))
    fn? (to-call data)
    (log/error "Not an effect or function: " to-call)))

(defn- effect-ws-request [{:keys [name params channel-key success error] :as request}]
  (let [args-hash (hash request)]
    (when-not (contains? @pending-requests args-hash)
      (swap! pending-requests conj args-hash)
      (send-request!
       {:name name
        :params params
        :channel-key channel-key
        :callback
        (fn [{data :data request-succeeded? :success :as response}]
          (swap! pending-requests disj args-hash)
          (cond
            (not request-succeeded?)
            (do
              (rf/dispatch [:ventas.components.notificator/add
                            {:message (or (:message data) data)
                             :theme "warning"}])
              (log/error "Request failed!" response)
              (when error
                (call error data)))
            success
            (call success data)))}))))

(defn- effect-ws-upload-request
  [{:keys [filename array-buffer success] :as request} & [file-id start]]
  (let [start (or start 0)
        data-length (-> array-buffer .-byteLength)
        raw-end (+ start ws-upload-chunk-size)
        last? (> raw-end data-length)
        first? (zero? start)
        end (if last? data-length raw-end)]
    (send-request!
     {:name ::api/upload
      :params {:bytes (.slice array-buffer start end)
               :last? last?
               :first? first?
               :file-id file-id
               :filename filename}
      :callback (fn [{:keys [data]}]
                  (if last?
                    (call success data)
                    (effect-ws-upload-request request
                                              (if first? data file-id)
                                              end)))}
     :binary? true)))

(defn requests-pending? []
  (boolean (seq @pending-requests)))

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

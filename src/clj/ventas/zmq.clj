(ns ventas.zmq
  (:require
   [zeromq.zmq :as zmq]
   [mount.core :refer [defstate]]
   [cognitect.transit :as transit]
   [taoensso.timbre :as timbre]
   [ventas.config :as config]))

(defstate zeromq
  :start
  (let [{:keys [host port]} (config/get :zeromq)
        url (str "tcp://" host ":" port)]
    (timbre/info "Starting zeromq at " url)
    (doto (zmq/socket (zmq/zcontext) :rep)
      (zmq/bind url)))
  :stop
  (do
    (timbre/info "Stopping zeromq")
    (.close zeromq)))
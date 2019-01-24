(ns ventas.search
  (:require
   [clojure.core.async :as core.async :refer [<! <!! >! >!! chan go go-loop]]
   [mount.core :refer [defstate]]
   [qbits.spandex :as spandex]
   [slingshot.slingshot :refer [throw+]]
   [ventas.config :as config]
   [ventas.utils :as utils]
   [clojure.tools.logging :as log])
  (:import
   (java.net ConnectException)))

(defn- get-url []
  (str "http://"
       (config/get :elasticsearch :host)
       ":"
       (config/get :elasticsearch :port)))

(defstate elasticsearch
  :start
  (let [url (get-url)]
    (log/info "Connecting to Elasticsearch at" url)
    (spandex/client {:hosts [url]})))

(def batch-size 5)

(defn make-url [& url]
  (let [index (config/get :elasticsearch :index)]
    (if url
      (apply str index "/" url)
      index)))

(defn- wrap-connect-exception [f]
  (try
    (f)
    (catch ConnectException _
      (throw+ {:type ::elasticsearch-unavailable
               :message (str "Could not make a Elasticsearch request; URL: " (get-url))}))))

(defn request [data]
  (wrap-connect-exception
   #(spandex/request elasticsearch data)))

(defn request-async [data]
  (wrap-connect-exception
   #(spandex/request-async elasticsearch data)))

(defn bulk-chan [config]
  (wrap-connect-exception
   #(spandex/bulk-chan elasticsearch config)))

(defn create-index [mapping]
  (log/debug mapping)
  (request {:url (make-url)
            :method :put
            :body mapping}))

(defn remove-index []
  (request {:url (make-url)
            :method :delete}))

(defn get-index []
  (request {:url (make-url)
            :method :get}))

(defn create-document [doc]
  (request {:url (make-url "doc")
            :method :post
            :body doc}))

(defn remove-document [id]
  (request {:url (make-url "doc/" id)
            :method :delete}))

(defn get-document [id]
  (request {:url (make-url "doc/" id)}))

(defn index-document [doc & {:keys [channel]}]
  {:pre [(map? doc)]}
  (log/debug :es-indexer doc)
  (let [f (if-not channel request request-async)]
    (f (merge {:url (make-url "doc/" (get doc "document/id"))
               :method :put
               :body (dissoc doc "document/id")}
              (when channel
                {:success #(core.async/put! channel %)
                 :error #(core.async/put! channel %)})))))

(defn search [q]
  (try
    (request {:url (make-url "_search")
              :body q})
    (catch Throwable e
      (let [message (get-in (ex-data e) [:body :error])]
        (log/error message)
        (throw+ {:type ::elasticsearch-error
                 :error message})))))

(defn- indexing-loop [indexing-chan]
  (future
   (let [{:keys [input-ch output-ch]} (bulk-chan {:flush-threshold 50
                                                  :flush-interval 3000
                                                  :max-concurrent-requests 3})]
     (go-loop []
       (when-not (Thread/interrupted)
         (let [[_ result] (<! output-ch)]
           (doseq [{:keys [index]} (:items (:body result))]
             (when (:error index)
               (log/error (:error index))))
           (recur))))
     (go-loop []
       (when-not (Thread/interrupted)
         (when-let [doc (<! indexing-chan)]
           (utils/interruptible-try
            (>! input-ch
                [{:index {:_index (config/get :elasticsearch :index)
                          :_type "doc"
                          :_id (get doc "document/id")}}
                 (dissoc doc "document/id")]))
           (recur)))))))

(defn start-indexer! []
  (let [indexing-chan (chan (core.async/buffer (* 10 batch-size)))
        indexing-future (indexing-loop indexing-chan)]
    {:future indexing-future
     :chan indexing-chan
     :stop-fn #(do (future-cancel indexing-future)
                   (core.async/close! indexing-chan))}))

(defstate indexer
  :start
  (do
    (log/info "Starting indexer")
    (start-indexer!))
  :stop
  (do
    (log/info "Stopping indexer")
    ((:stop-fn indexer))))

(defn document->indexing-queue [doc]
  (core.async/put! (:chan indexer) doc))
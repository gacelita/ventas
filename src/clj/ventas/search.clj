(ns ventas.search
  (:require
   [clojure.core.async :refer [<! >! go go-loop chan] :as core.async]
   [qbits.spandex :as spandex]
   [mount.core :as mount :refer [defstate]]
   [ventas.config :as config]
   [ventas.database :as db]
   [taoensso.timbre :as timbre]
   [ventas.database.entity :as entity]
   [ventas.utils :as utils]
   [clojure.string :as str]
   [ventas.common.utils :as common.utils]
   [ventas.events :as events]))

(def c (spandex/client {:hosts [(str "http://"
                                     (config/get :elasticsearch :host)
                                     ":"
                                     (config/get :elasticsearch :port))]}))

(def batch-size 5)

(defn- make-url [& url]
  (let [index (config/get :elasticsearch :index)]
    (if url
      (apply str index "/" url)
      index)))

(defn create-index [mapping]
  (timbre/debug mapping)
  (spandex/request c {:url (make-url)
                      :method :put
                      :body {:mappings {:doc mapping}}}))

(defn remove-index []
  (spandex/request c {:url (make-url)
                      :method :delete}))

(defn get-index []
  (spandex/request c {:url (make-url)
                      :method :get}))


(defn create-document [doc]
  (spandex/request c {:url (make-url "doc")
                      :method :post
                      :body doc}))

(defn remove-document [doc]
  (spandex/request c {:url (make-url "doc")
                      :method :delete}))

(defn get-document [id]
  (spandex/request c {:url (make-url "doc/" id)}))

(defn index-document [doc & {:keys [channel]}]
  {:pre [(map? doc)]}
  (timbre/debug "Indexing document" doc)
  (let [f (if-not channel
            spandex/request
            spandex/request-async)]
    (f c (merge {:url (make-url "doc/" (get doc "db/id"))
                 :method :put
                 :body (dissoc doc "db/id")}
                (when channel
                  {:success (fn [res]
                              (go (>! channel res)))
                   :error (fn [ex]
                            (go (>! channel ex)))})))))

(defn search [q]
  (spandex/request c {:url (make-url "_search")
                      :body q}))

(defn- ident->property [ident]
  (str/replace (str (namespace ident) "/" (name ident))
               "."
               "__"))

(defn- value-type->es-type [type]
  (case type
    :db.type/bigdec "long"
    :db.type/boolean "boolean"
    :db.type/float "double"
    :db.type/keyword "keyword"
    :db.type/long "long"
    :db.type/ref "text"
    :db.type/string "text"
    "text"))

(defn- attributes->mapping [attrs]
  (->> attrs
       (map (fn [{:db/keys [ident valueType]}]
              (when valueType
                [(ident->property ident)
                 {:type (value-type->es-type valueType)}])))
       (into {})))

(defn setup []
  (create-index {:properties (attributes->mapping (db/schema))}))

(defonce ^:private indexing-queue (chan (core.async/buffer (* 10 batch-size))))

(defn document->indexing-queue [doc]
  (go (>! indexing-queue doc)))

(defn- resolve-i18n [v]
  (if (number? v)
    (let [entity (entity/find v)]
      (if (and entity (= (:schema/type entity) :schema.type/i18n))
        (->> (entity/to-json entity)
             (map (fn [[culture value]]
                    [(->> culture entity/find :i18n.culture/keyword)
                     value]))
             (into {}))
        v))
    v))

(defn- filter-entity-attr [e [a v]]
  (if-not (map? v)
    (assoc e a v)
    (merge e
           (->> v
                (common.utils/map-keys
                 #(keyword (namespace a)
                           (str (name a) "__" (name %))))))))

(defn- index-entity [eid]
  (let [doc (->> (entity/find eid)
                 (common.utils/map-vals resolve-i18n)
                 (reduce filter-entity-attr
                         {})
                 (common.utils/map-keys ident->property))]
    (document->indexing-queue doc)))

(defn reindex
  "Indexes everything"
  []
  (remove-index)
  (setup)
  (let [types (->> @ventas.database.entity/registered-types
                   (filter (fn [[k v]]
                             (not (:component? v))))
                   (keys))]
    (doseq [type types]
      (let [entities (entity/query type)]
        (doseq [{:db/keys [id]} entities]
          (index-entity id))))))

(defn- index-report [{:keys [db-after tx-data]}]
  (let [types (->> (keys @ventas.database.entity/registered-types)
                   (map name)
                   (set))
        eids (->> tx-data
                  (map db/datom->map)
                  (filter (fn [{:keys [e a]}]
                            (let [type (namespace a)]
                              (and (not= type "event")
                                   (contains? types type)))))
                  (map :e))]
    (doseq [eid eids]
      (index-entity eid))))

(defstate indexer
  :start
  (do
    (timbre/info "Starting indexer")
    (let [indexer-ch (chan)
          batch-ch (chan)]
      (go-loop []
        (let [report-ch (db/tx-report-queue-async)
              [message ch] (core.async/alts! [indexer-ch report-ch])]
          (when (= ch report-ch)
            (index-report message))
          (when-not (nil? message)
            (recur))))
      (go-loop []
        (let [[message ch] (core.async/alts! [indexer-ch batch-ch])]
          (when (and (= ch batch-ch) (seq message))
            (let [response-ch (chan)]
              (taoensso.timbre/debug "got msg" message)
              (doseq [doc message]
                (index-document doc :channel response-ch))
              (doseq [doc message]
                (<! response-ch))))
          (when-not (nil? message)
            (recur))))
      (utils/batch indexing-queue
                   batch-ch
                   4000
                   5)
      indexer-ch))
  :stop
  (do
    (timbre/info "Stopping indexer")
    (core.async/close! indexer)))
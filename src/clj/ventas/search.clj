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

(defstate elasticsearch
  :start
  (let [address (str "http://"
                     (config/get :elasticsearch :host)
                     ":"
                     (config/get :elasticsearch :port))]
    (timbre/info "Connecting to Elasticsearch at" address)
    (spandex/client {:hosts [address]})))

(def batch-size 5)

(defn make-url [& url]
  (let [index (config/get :elasticsearch :index)]
    (if url
      (apply str index "/" url)
      index)))

(defn request [data]
  (spandex/request elasticsearch data))

(defn request-async [data]
  (spandex/request-async elasticsearch data))

(defn create-index [mapping]
  (timbre/debug mapping)
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

(defn remove-document [doc]
  (request {:url (make-url "doc")
            :method :delete}))

(defn get-document [id]
  (request {:url (make-url "doc/" id)}))

(defn index-document [doc & {:keys [channel]}]
  {:pre [(map? doc)]}
  (timbre/debug "Indexing document" doc)
  (let [f (if-not channel request request-async)]
    (f (merge {:url (make-url "doc/" (get doc "db/id"))
               :method :put
               :body (dissoc doc "db/id")}
              (when channel
                {:success (fn [res]
                            (go (>! channel res)))
                 :error (fn [ex]
                          (go (>! channel ex)))})))))

(defn search [q]
  (request {:url (make-url "_search")
            :body q}))

(defn- ident->property [ident]
  {:pre [(keyword? ident)]}
  (str/replace (str (namespace ident) "/" (name ident))
               "."
               "__"))

(defn- value-type->es-type [type ref-entity-type]
  (case type
    :db.type/bigdec "long"
    :db.type/boolean "boolean"
    :db.type/float "double"
    :db.type/keyword "keyword"
    :db.type/long "long"
    :db.type/ref (case ref-entity-type
                   :i18n "text"
                   :enum "keyword"
                   "long")
    :db.type/string "text"
    "text"))

(def ^:private autocomplete-idents
  #{:product/name
    :category/name
    :brand/name})

(defn- with-culture [ident culture-kw]
  (keyword (namespace ident)
           (str (name ident) "." (name culture-kw))))

(defn- attributes->mapping [attrs]
  (let [culture-kws (->> (entity/query :i18n.culture)
                         (map :i18n.culture/keyword))]
    (->> attrs
         (filter :db/valueType)
         (map (fn [{:db/keys [ident valueType] :ventas/keys [refEntityType] :as attr}]
                {:attr attr
                 :value
                 (let [type (value-type->es-type valueType refEntityType)]
                   (merge {:type type}
                          (when (contains? autocomplete-idents ident)
                            {:analyzer "autocomplete"
                             :search_analyzer "standard"})))}))
         (reduce (fn [acc {:keys [attr value]}]
                   (let [{:ventas/keys [refEntityType] :db/keys [ident]} attr]
                     (if-not (= refEntityType :i18n)
                       (assoc acc (ident->property ident) value)
                       (merge acc
                              (->> culture-kws
                                   (map (fn [culture-kw]
                                          [(ident->property (with-culture ident culture-kw))
                                           value]))
                                   (into {}))))))
                 {}))))

(defn setup []
  (create-index {:mappings {:doc {:properties (attributes->mapping (db/schema))}}
                 :settings {:analysis {:filter {:autocomplete_filter {:type "edge_ngram"
                                                                      :min_gram 1
                                                                      :max_gram 20}}
                                       :analyzer {:autocomplete {:type "custom"
                                                                 :tokenizer "standard"
                                                                 :filter ["lowercase"
                                                                          "autocomplete_filter"]}}}}}))

(def ^:private indexing-queue
  (atom (chan (core.async/buffer (* 10 batch-size)))))

(defn document->indexing-queue [doc]
  (go (>! @indexing-queue doc)))

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

(defn i18n-field [field culture-kw]
  (keyword (namespace field)
           (str (name field)
                "__" (name culture-kw))))

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
              (doseq [doc message]
                (index-document doc :channel response-ch))
              (doseq [doc message]
                (let [result (<! response-ch)]
                  (when (instance? clojure.lang.ExceptionInfo result)
                    (taoensso.timbre/error (get-in (ex-data result)
                                                   [:body :error])))))))
          (when-not (nil? message)
            (recur))))
      (utils/batch @indexing-queue
                   batch-ch
                   4000
                   5)
      indexer-ch))
  :stop
  (do
    (timbre/info "Stopping indexer")
    (core.async/close! indexer)))
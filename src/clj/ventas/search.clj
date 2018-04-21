(ns ventas.search
  (:require
   [clojure.core.async :as core.async :refer [<! <!! >! >!! chan go go-loop]]
   [clojure.string :as str]
   [mount.core :refer [defstate]]
   [qbits.spandex :as spandex]
   [slingshot.slingshot :refer [throw+]]
   [taoensso.timbre :as timbre]
   [ventas.common.utils :as common.utils]
   [ventas.config :as config]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.entities.category :as entities.category]
   [ventas.utils :as utils])
  (:import
   [clojure.lang ExceptionInfo]))

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
  (timbre/debug :es-indexer doc)
  (let [f (if-not channel request request-async)]
    (f (merge {:url (make-url "doc/" (get doc "db/id"))
               :method :put
               :body (dissoc doc "db/id")}
              (when channel
                {:success #(core.async/put! channel %)
                 :error #(core.async/put! channel %)})))))

(defn- indexing-loop [indexing-chan]
  (future
   (let [batch-output-chan (chan)]
     (utils/batch indexing-chan
                  batch-output-chan
                  4000
                  5)
     (loop []
       (when-not (Thread/interrupted)
         (try
           (let [docs (<!! batch-output-chan)]
             (when (seq docs)
               (let [response-ch (chan)]
                 (doseq [doc docs]
                   (index-document doc :channel response-ch))
                 (doseq [doc docs]
                   (let [result (<!! response-ch)]
                     (when (instance? ExceptionInfo result)
                       (timbre/error (get-in (ex-data result) [:body :error]))))))))
           (catch InterruptedException _
             (.interrupt (Thread/currentThread)))
           (catch Throwable e
             (timbre/error (class e) (.getMessage e))))
         (recur))))))

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
    (timbre/info "Starting indexer")
    (start-indexer!))
  :stop
  (do
    (timbre/info "Stopping indexer")
    ((:stop-fn indexer))))

(defn document->indexing-queue [doc]
  (core.async/put! (:chan indexer) doc))

(defn search [q]
  (try
    (request {:url (make-url "_search")
              :body q})
    (catch Throwable e
      (let [message (get-in (ex-data e) [:body :error])]
        (taoensso.timbre/error message)
        (throw+ {:type ::elasticsearch-error
                 :error message})))))

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

(defn- ref->es [{:schema/keys [type] :as entity}]
  (case type
    :schema.type/i18n
    (->> (entity/serialize entity)
         (map (fn [[culture value]]
                [(->> culture entity/find :i18n.culture/keyword)
                 value]))
         (into {}))
    :schema.type/amount
    (:amount/value entity)

    (:db/id entity)))

(defn- value->es [a v]
  (cond
    (= a :product/categories)
    (set (mapcat entities.category/get-parents v))

    (number? v)
    (if-let [entity (entity/find v)]
      (ref->es entity)
      v)

    :default v))

(defn- filter-entity-attr [e [a v]]
  (if-not (map? v)
    (assoc e a v)
    (merge e
           (->> v
                (common.utils/map-keys
                 #(keyword (namespace a)
                           (str (name a) "__" (name %))))))))

(defn index-entity [eid]
  (let [doc (->> (entity/find eid)
                 (map (fn [[a v]]
                        [a (value->es a v)]))
                 (reduce filter-entity-attr
                         {})
                 (common.utils/map-keys ident->property))]
    (document->indexing-queue doc)))

(defn- indexable-types []
  (->> @ventas.database.entity/registered-types
       (filter (fn [[k v]]
                 (not (:component? v))))
       (keys)))

(defn reindex
  "Indexes everything"
  []
  (utils/swallow
   (remove-index))
  (setup)
  (let [types (indexable-types)]
    (doseq [type types]
      (let [entities (entity/query type)]
        (doseq [{:db/keys [id]} entities]
          (index-entity id))))))

(defn i18n-field [field culture-kw]
  (keyword (namespace field)
           (str (name field)
                "__" (name culture-kw))))

(defn- index-report [{:keys [tx-data]}]
  (let [types (->> (indexable-types)
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

(defn start-tx-report-queue-listener! []
  (future
   (loop []
     (when-not (Thread/interrupted)
       (try
         (when-let [report (.take (db/tx-report-queue))]
           (index-report report))
         (catch InterruptedException _
           (.interrupt (Thread/currentThread)))
         (catch Throwable e
           (timbre/error (class e) (.getMessage e))))
       (recur)))))

(defstate tx-report-queue-listener
  :start
  (do
    (timbre/info "Starting tx-report-queue listener")
    (start-tx-report-queue-listener!))
  :stop
  (do
    (timbre/info "Stopping tx-report-queue listener")
    (future-cancel tx-report-queue-listener)))

(defn- prepare-search-attrs [attrs culture-kw]
  (for [attr attrs]
    (let [{:ventas/keys [refEntityType]} (db/touch-eid attr)]
      (cond
        (= refEntityType :i18n)
        (keyword (namespace attr)
                 (str (name attr) "__" (name culture-kw)))

        :else attr))))

(defn entities
  "Fulltext search for `search` in the given `attrs`"
  [text attrs culture-id]
  {:pre [(utils/check ::entity/ref culture-id)]}
  (let [culture (entity/find culture-id)]
    (let [shoulds (for [attr (prepare-search-attrs attrs (:i18n.culture/keyword culture))]
                    {:match {attr text}})
          hits (-> (search {:query {:bool {:should shoulds}}
                            :_source false})
                   (get-in [:body :hits :hits]))]
      (->> hits
           (map :_id)
           (map (fn [v] (Long/parseLong v)))
           (map #(entity/find-serialize % {:culture (:db/id culture)
                                           :keep-type? true}))))))

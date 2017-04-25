(ns ventas.database
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [clojure.pprint :as p]
    [datomic.api :as d]
    [adi.core :as adi]
    [buddy.hashers :as hashers]
    [mount.core :as mount :refer [defstate]]
    [ventas.config :refer [config]]
    [ventas.util :as util :refer [print-info]]
    [slingshot.slingshot :refer [throw+ try+]]
    [clojure.spec :as s]
    [clojure.spec.test :as stest]
    [clojure.test.check.generators :as gen]
    [com.gfredericks.test.chuck.generators :as gen']
    [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(defn entity-spec [data]
  (if (s/valid? (:schema/type data) data)
     data
     (throw+ {:type ::spec-invalid :message (s/explain (:schema/type data) data)})))

(defn generate-1 [spec]
  (gen/generate (s/gen spec)))

(defn generate-n [spec n]
  "Generates n samples of given spec"
  (let [generator (s/gen spec)]
    (map (fn [_] (gen/generate generator)) (range n))))

;; ADI schema
;; Technically not required, but useful for clarity and for the REPL

(def schema {})

(defmacro defentity [type this-schema]
  (let [kwtype (keyword (clojure.string/lower-case (name type)))]
    `(do
      (~'defrecord ~type [~@(map (fn defentity-map [k] (symbol (name k))) (keys this-schema))])
    (~'def ~'schema (~'assoc ~'schema ~kwtype ~this-schema)))))

(defentity User {
  :name [{:required true :index true :fulltext true}]
  :password [{:required true}]
  :email [{:required true :unique :value}]
  :avatar [{:type :ref}]
  :status [{:type :enum :default :active :enum {:ns :user.status :values #{:pending :active :inactive :cancelled}}}]
  :description [{:type :string}]
  :roles [{:type :enum :default :user :enum {:ns :user.role :values #{:administrator :user}}}]
})
 
(defentity Image {
  :extension [{:required true}]
  :source [{:type :ref}]
})
          
(defentity ImageTag {
  :image [{:type :ref}] 
  :target [{:type :ref}]
  :x [{:type :long}]
  :y [{:type :long}]
  :caption [{:type :string}]
})

(defentity Comment {
  :source [{:type :ref :required true}]
  :content [{:required true :fulltext true}]
  :target [{:type :ref :required true}]
})

(defentity Friendship {
  :source [{:type :ref :required true}]
  :target [{:type :ref :required true}]
})

;; Connection lifecycle

(defn start-db! []
  (print-info "Starting database")
  (adi/connect! (get-in config [:database :url]) schema false false))
(defn stop-db! [db]
  (print-info "Stopping database"))
(defstate db :start (start-db!) :stop (stop-db! db))


;; Random database utility functions

(defn get-partitions []
  (d/q '[:find ?ident :where [:db.part/db :db.install/partition ?p]
                             [?p :db/ident ?ident]]
       (d/db (:connection db))))

(defn entity-dates [eid]
  (first (d/q '[:find (min ?tx-time) (max ?tx-time)
                :in $ ?e
                :where
                  [?e _ _ ?tx _]
                  [?tx :db/txInstant ?tx-time]]
            (d/history (d/db (:connection db))) eid)))

(defn assoc-derived-fields [data eid]
  (let [dates (entity-dates eid)]
    (-> data
        (assoc :id eid)
        (assoc :created-at (get dates 0))
        (assoc :updated-at (get dates 1)))))

(defn touch-eid [eid]
  (into {} (d/touch (d/entity (d/db (:connection db)) eid))))

(defn touch-eid-assoc-fields [eid]
  (assoc-derived-fields (touch-eid eid) eid))


(defn entity-record [type data]
  "Entity map -> entity record"
  (let [record-fn-symbol (symbol (str "map->" (clojure.string/join (map clojure.string/capitalize (clojure.string/split (name type) #"\.")))))
        record-fn (ns-resolve 'ventas.database record-fn-symbol)]
    (when-not (ifn? record-fn)
      (throw+ {:type ::not-a-function :symbol record-fn-symbol :message "This symbol does not exist or is not a function"}))
    (record-fn data)))

(declare process-result)

(defn resolve-subentities [m]
  (into {}
    (for [[k v] m]
      [k (if (instance? datomic.query.EntityMap v) (process-result (touch-eid-assoc-fields (:db/id v))) v)])))

(defn identify-subentities [m]
  (into {}
    (for [[k v] m]
      [k (if (instance? datomic.query.EntityMap v) (:db/id v) v)])))


(defn process-result [data]
  (let [dequalified (util/dequalify-keywords data)
        with-subentities (identify-subentities dequalified)]
    (entity-record (:type with-subentities) with-subentities)))

(defn process-transaction [type tx tempid]
  (process-result (resolve-subentities (touch-eid-assoc-fields (d/resolve-tempid (d/db (:connection db)) (:tempids tx) tempid)))))

(defn get-schema []
  (let [system-ns #{"db" "db.alter" "db.sys" "db.type" "db.install" "db.part" 
                    "db.lang" "fressian" "db.unique" "db.excise" "db.cardinality" "db.fn"}]
    (map touch-eid
      (sort (d/q '[:find [?ident ...]
                   :in $ ?system-ns
                   :where [?e :db/ident ?ident]
                          [(namespace ?ident) ?ns]
                          [((comp not contains?) ?system-ns ?ns)]
                          [_ :db.install/attribute ?e]]
                  (d/db (:connection db)) system-ns)))))

(defn get-enum-values [enum]
  (d/q '[:find ?id ?ident ?value
         :in $ ?enum
         :where [?id :db/ident ?ident]
                [(name ?ident) ?value]
                [(namespace ?ident) ?ns]
                [(= ?ns ?enum)]] (d/db (:connection db)) enum))

(defn keyword-to-db-symbol [keyword]
  (symbol (str "?" (name keyword))))

(defn retract-entity [eid]
  "Retract an entity by eid"
  @(d/transact (:connection db) [[:db.fn/retractEntity eid]]))

(defn pull [& args]
  (apply d/pull (concat [(d/db (:connection db))] args)))

(defn read-changes [{:keys [db-after tx-data] :as report} query]
  "Given a report from tx-report-queue and a query, gets the changes"
  (d/q query
       db-after
       tx-data))

(defn tx-report-queue [] (d/tx-report-queue db))

(defn filtered-query
  "Filtered query.
   Usage: (filtered-query (quote ([?id :user/email ?email])) {:email \"some-email@example.com\"})"
  ([] (filtered-query '() {}))
  ([wheres] (filtered-query wheres {}))
  ([wheres filters]
   (let [ins (remove nil? (concat [(symbol "$")] (map keyword-to-db-symbol (keys filters))))
         query (vec (concat '(:find ?id)
                  '(:in) ins
                  '(:where) wheres))]
     (apply d/q (concat [query (d/db (:connection db))] (vals filters))))))

(defn entity-filtered-query
  ([type filters]
    (entity-filtered-query type (map (fn [[k v]] [ '?id (if (namespace k) k (util/qualify-keyword k type)) (if (= v :any) '_ (keyword-to-db-symbol k))]) filters) filters))
  ([type wheres filters]
    (filtered-query wheres filters)))


;; Hooks

(declare entity-query)

(defmulti entity-precreate (fn entity-precreate [data] (keyword (name (:schema/type data)))))
(defmethod entity-precreate :default [data] data)
(defmethod entity-precreate :user [data]
  (if-not (:user/status data)
    (assoc data :user/status :user.status/active)
    data))

(defmulti entity-postcreate (fn entity-postcreate [type entity] type))
(defmethod entity-postcreate :default [type entity] true)

(defmulti entity-predelete (fn entity-predelete [type entity] type))
(defmethod entity-predelete :default [type entity] true)

(defmulti entity-postdelete (fn entity-postdelete [type entity] type))
(defmethod entity-postdelete :default [type entity] true)

(defmulti entity-preupdate (fn entity-preupdate [type entity params] type))
(defmethod entity-preupdate :default [type entity params] true)

(defmulti entity-postupdate (fn entity-postupdate [type entity params] type))
(defmethod entity-postupdate :default [type entity params] true)

(defmulti entity-postquery (fn entity-postquery [entity] (keyword (name (:type entity)))))
(defmethod entity-postquery :default [entity] entity)
(defmethod entity-postquery :image [entity] 
  (-> entity
      (assoc :url (str (:base-url config) "img/" (:id entity) "." (name (:extension entity))))
      (assoc :tags (entity-query :image.tag {:image (:id entity)}))))
  


;; Querying

(defmulti entity-query
  (fn entity-query
    ([type] type)
    ([type params] type)
    ([type wheres params] type)))

(defmethod entity-query :default
  ([type] (entity-query type {:schema/type (keyword "schema.type" (name type))}))
  ([type params]
    (let [results (entity-filtered-query type params)]
      (map (fn [[eid]] (entity-postquery (process-result (touch-eid-assoc-fields eid)))) results)))
  ([type wheres params]
    (let [results (entity-filtered-query type wheres params)]
      (map (fn [[eid]] (entity-postquery (process-result (touch-eid-assoc-fields eid)))) results))))



(defn entity-find [eid]
  (process-result (resolve-subentities (touch-eid-assoc-fields eid))))


;; Creating




(defmulti entity-create (fn entity-create [type params] type))
(defmethod entity-create :default [type params]
  (let [tempid (d/tempid :db.part/user)
        tx @(d/transact (:connection db) [(-> (util/qualify-map-keywords (util/filter-empty-vals params) type)
                                              (assoc :db/id tempid)
                                              (assoc :schema/type (keyword "schema.type" (name type)))
                                              entity-precreate
                                              entity-spec)])
        entity (process-transaction type tx tempid)]
    (entity-postcreate type entity)
    entity))

;; Base

(defprotocol EntityType
  (entity-type [this]))

;; Update and delete

(defprotocol EntityUpdate
  (entity-update [this params]))

(defprotocol EntityDelete
  (entity-delete [this]))

;; Default implementation

(extend-type Object
  EntityType
    (entity-type [this]
      (-> this class .getSimpleName clojure.string/lower-case keyword))
  EntityUpdate
    (entity-update [this params]
      (when (nil? (:id this))
        (throw+ {:type ::invalid-update :entity this :message "The entity needs to have an ID in order to be updated"}))
      (entity-preupdate (entity-type this) this params)
      @(d/transact (:connection db) [(assoc (util/qualify-map-keywords params (entity-type this)) :db/id (:id this))])
      (entity-postupdate (entity-type this) this params)
      (entity-find (:id this)))
  EntityDelete
  (entity-delete [this]
    (when (nil? (:id this))
      (throw+ {:type ::invalid-deletion :entity this :message "The entity needs to have an ID in order to be deleted"}))
    (entity-predelete (entity-type this) this)
    (retract-entity (:id this))
    (entity-postdelete (entity-type this) this)
    (:id this)))


(defn entity-upsert [type data]
  (if (:id data)
    (entity-update (entity-record type (entity-find (:id data))) (dissoc data :id))
    (entity-create type data)))

;; Intended usage:
;; (def users (entity-query :user {:email "something@something.com"}))
;; (entity-delete user)
;; (entity-update user {:email "othersomething@something.com"})
;; (entity-create :user {:username "Something" :email "something@something.com"})



(s/def :user/name string?)
(s/def :user/password string?)
(s/def :user/description string?)
(s/def :user/status #{:user.status/pending :user.status/active :user.status/inactive :user.status/cancelled})
(s/def :user/roles #{:user.role/administrator :user.role/user})
(s/def :user/email
  (s/with-gen (s/and string? #(re-matches #"^.+@.+$" %))
              #(gen'/string-from-regex #"[a-z0-9]{3,6}@[a-z0-9]{3,6}\.(com|es|org)")))
(s/def :schema.type/user
  (s/keys :req [:user/name :user/password :user/email :user/status]
          :opt [:user/description :user/roles]))


(s/def :comment/source
  (s/with-gen integer? #(gen/elements (map :id (entity-query :user)))))
(s/def :comment/target
  (s/with-gen integer? #(gen/elements (map :id (entity-query :user)))))
(s/def :comment/content string?)
(s/def :schema.type/comment
  (s/keys :req [:comment/source :comment/target :comment/content]))

(s/def :image/extension #{:image.extension/jpg :image.extension/gif :image.extension/png :image.extension/tiff})
(s/def :image/source
  (s/with-gen integer? #(gen/elements (map :id (entity-query :user)))))
(s/def :schema.type/image
  (s/keys :req [:image/extension :image/source]))

(s/def :image.tag/image
  (s/with-gen integer? #(gen/elements (map :id (entity-query :image)))))
(s/def :image.tag/target
  (s/with-gen integer? #(gen/elements (map :id (entity-query :user)))))
(s/def :image.tag/x
  (s/with-gen integer? #(gen/choose 100 1000)))
(s/def :image.tag/y
  (s/with-gen integer? #(gen/choose 100 1000)))
(s/def :image.tag/caption string?)
(s/def :schema.type/image.tag
  (s/with-gen (s/keys :req [:image.tag/image :image.tag/target]
                      :opt [:image.tag/x :image.tag/y :image.tag/caption])
              #(s/gen (s/keys :req [:image.tag/image :image.tag/target :image.tag/x :image.tag/y :image.tag/caption]))))

(defn seed []
  "Seeds the database with sample data"
  (println "Seeding users")
  (doseq [user (generate-n :schema.type/user 10)] (entity-create :user user))
  (println "Seeding comments")
  (doseq [comment (generate-n :schema.type/comment 100)] (entity-create :comment comment))
  (println "Seeding friendships")
  (doseq [friendship (generate-n :schema.type/friendship 40)] (entity-create :friendship friendship))
  (println "Seeding images")
  (doseq [image (generate-n :schema.type/image 50)] (entity-create :image image))
  (println "Seeding tags")
  (doseq [imagetag (generate-n :schema.type/image.tag 150)] (entity-create :image.tag imagetag)))
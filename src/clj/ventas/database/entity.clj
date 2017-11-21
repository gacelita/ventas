(ns ventas.database.entity
  (:refer-clojure :exclude [find type update])
  (:require
   [clojure.core :as clj]
   [clojure.core.async :refer [go <! go-loop]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [datomic.api :as d]
   [slingshot.slingshot :refer [throw+ try+]]
   [taoensso.timbre :as timbre :refer [trace debug info warn error]]
   [ventas.database :as db]
   [ventas.database.schema :as schema]
   [ventas.events :as events]
   [ventas.util :as util]))

(defn is-entity? [entity]
  (when (map? entity)
    (contains? (set (keys entity)) :schema/type)))

(spec/def ::entity-type
  (spec/keys :opt-un [::attributes
                      ::to-json
                      ::filter-seed
                      ::filter-transact
                      ::filter-update
                      ::before-seed
                      ::before-transact
                      ::before-delete
                      ::after-seed
                      ::after-transact
                      ::after-delete]))

(defonce registered-types (atom {}))

(defn register-type!
  "Registers an entity type
   Example: (register-entity-type! :user)"
  [kw & [m]]
  {:pre [(keyword? kw) (or (nil? m)
                           (and (map? m) (util/check ::entity-type m)))]}
  (let [m (or m {})]
    (schema/register-migration!
     [{:db/ident (db/kw->type kw)}])
    (schema/register-migration!
     (or (:attributes m) []))
    (swap! registered-types assoc kw m)))

(defn types
  "Returns all types"
  []
  (set (keys @registered-types)))

(defn type-exists?
  [type]
  {:pre [(keyword? type)]}
  (not (nil? (get @registered-types type))))

(defn type
  "Returns the type of an entity"
  [entity]
  {:pre [(is-entity? entity)]}
  (keyword (name (:schema/type entity))))

(defn type-fns
  "Returns the functions of an entity type"
  [type]
  (get @registered-types type))

(def default-type
  {:attributes []
   :to-json (fn [entity]
              (-> entity
                  (dissoc :schema/type)
                  (util/dequalify-keywords)))
   :filter-seed identity
   :filter-transact identity
   :filter-update (fn [entity data] data)
   :before-seed (fn [_] true)
   :before-transact (fn [_] true)
   :before-delete (fn [_] true)
   :after-seed (fn [_] true)
   :after-transact (fn [_] true)
   :after-delete (fn [_] true)})

(defn- call-type-fn [kw entity & args]
  (let [type-fns (type-fns (type entity))
        type-fn (if (kw type-fns)
                   (kw type-fns)
                   (kw default-type))]
    (apply type-fn entity args)))

(defn to-json
  "Transforms an entity into a stripped map, suitable for sending to the outside"
  [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :to-json entity))

(defn filter-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-seed entity))

(defn filter-transact [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-transact entity))

(defn filter-update [entity data]
  {:pre [(is-entity? entity) (map? data)]}
  (call-type-fn :filter-update entity data))

(defn before-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-seed entity))

(defn before-transact [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-transact entity))

(defn before-delete [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-delete entity))

(defn after-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-seed entity))

(defn after-transact [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-transact entity))

(defn after-delete [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-delete entity))

(defn spec
  "Checks that an entity complies with its spec"
  [entity]
  {:pre [(type-exists? (type entity))]}
  (let [spec (:schema/type entity)]
    (when (util/spec-exists? spec)
      (util/check spec entity))))

(defn find
  "Finds an entity by eid or lookup ref"
  [eid]
  (db/touch-eid eid))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (-> (db/resolve-tempid (:tempids tx) tempid)
      (find)))

(defn- prepare-pre-entity [pre-entity & [initial-tempid]]
  (into {}
        (map (fn [[k v]]
               [k (cond
                    (is-entity? v) (prepare-pre-entity v)
                    (and (coll? v) (is-entity? (first v))) (map prepare-pre-entity v)
                    :else v)])
             (-> pre-entity
                 (assoc :db/id (or initial-tempid (d/tempid :db.part/user)))
                 (filter-transact)))))

(defn transact
  "Transacts an entity"
  [pre-entity]
  {:pre [(do (println pre-entity) true) (spec pre-entity)]}
  (before-transact pre-entity)
  (let [tempid (d/tempid :db.part/user)
        pre-entity (prepare-pre-entity pre-entity tempid)
        tx (db/transact [pre-entity])
        entity (transaction->entity tx tempid)]
    (after-transact entity)
    entity))

(defn create
  "Creates an entity and transacts it.
   Example usage:
   (create :user {:name `Joel` :email `test@test.com`})"
  [type attributes]
  (let [entity
        (-> (util/qualify-map-keywords (util/filter-empty-vals attributes) type)
            (assoc :schema/type (db/kw->type type)))]
    (transact entity)))

(defn from-json
  "Inverse of to-json"
  [attributes]
  {:pre [(map? attributes)]}
  (let [id (:id attributes)
        type (:type attributes)]
    (-> attributes
        (dissoc :id)
        (dissoc :type)
        (util/qualify-map-keywords type)
        (assoc :schema/type type)
        (assoc :db/id id))))

(defn attributes
  [type]
  (:attributes (type-fns type)))

(defn fixtures
  [type]
  (when-let [fixtures-fn (:fixtures (type-fns type))]
    (fixtures-fn)))

(defn dependencies
  [type]
  (or (:dependencies (type-fns type)) #{}))

(defn seed-number
  [type]
  (or (:seed-number (type-fns type)) 30))

(defn attributes-by-ident
  [type]
  "Returns a map whose keys are idents and whose values are valueTypes of those idents"
  (into {}
        (map
         (fn [attr]
           [(:db/ident attr) attr])
         (attributes type))))

(defn update
  "Takes an eid and a list of attributes to transact.
   Example usage:
   (update 1234567 {:name `Other name`})"
  [eid attrs]
  {:pre [(map? attrs) (number? eid)]}
  (let [entity (find eid)
        entity-type (type entity)
        data (as-> attrs data
                   (filter-update entity data)
                   (dissoc data :id)
                   (dissoc data :type)
                   (util/qualify-map-keywords data entity-type)
                   (assoc data :db/id (:db/id entity))
                   (assoc data :schema/type (db/kw->type entity-type)))
        enum-retractions
        (let [relevant-attrs (filter #(contains? (set (keys data)) (:db/ident %))
                                     (attributes entity-type))
              enum-attrs (filter #(and (= (:db/cardinality %) :db.cardinality/many)
                                       (= (:db/valueType %) :db.type/ref))
                                 relevant-attrs)
              enum-idents (map :db/ident enum-attrs)]
          (mapcat (fn [{:keys [ident new-val]}]
                 (let [diff (set/difference (set (get entity ident))
                                            (set new-val))]
                   (map (fn [val-to-retract]
                          [:db/retract (:db/id data) ident val-to-retract])
                        diff)))
               (map #(hash-map :ident % :new-val (get data %))
                    enum-idents)))]
    (db/transact (concat [data] enum-retractions))
    (find eid)))

(defn delete [eid]
  "Deletes an entity by eid"
  (let [entity (find eid)]
    (before-delete entity)
    (db/retract-entity eid)
    (after-delete entity)))

(defn upsert
  "Like create, but if the list of attributes contains an id, it is used as
   the argument for `update`"
  [type {:keys [id] :as attributes}]
  {:pre [(keyword? type) (map? attributes)]}
  (if id
    (update id attributes)
    (create type attributes)))

(defn dates
  "First and last dates associated with an eid"
  [eid]
  (let [query '[:find (min ?tx-time) (max ?tx-time)
                :in $ ?e
                :where [?e _ _ ?tx _]
                       [?tx :db/txInstant ?tx-time]]
        result (first (d/q query (db/history) eid))]
    {:created-at (get result 0)
     :updated-at (get result 1)}))

(defn- filters->wheres [type filters]
  (->> filters
       (map (fn [[attribute value]]
              (let [attribute (if (namespace attribute)
                                attribute
                                (util/qualify-keyword attribute type))
                    value (if (= value :any) '_ value)]
                ['?id attribute value])))
       (mapcat (fn [[var attribute value]]
                 (if (set? value)
                   (for [item value]
                     [var attribute item])
                   [[var attribute value]])))
       (mapcat (fn [[var attribute value]]
                 (if (vector? value)
                   (let [[min max] value]
                     [[var attribute value]])
                   [[var attribute value]])))))

(defn query
  "Performs a high-level query.
   Accepts optional `wheres` clauses"
  [type & [filters]]
  (let [filters (filter (fn [[k v]] v) filters)
        filters (if (empty? filters)
                  {:schema/type (db/kw->type type)}
                  filters)]
    (map #(find (:id %))
         (db/nice-query {:find '[?id]
                         :where (filters->wheres type filters)}))))

(spec/def
  ::ref
  (spec/or :eid number?
           :entity is-entity?
           :lookup-ref vector?))

(defn ref-generator [type]
  (gen/elements (map :db/id (query type))))

(spec/def
  ::refs
  (spec/coll-of ::ref))

(defn refs-generator [type]
  (gen/vector (ref-generator type)))

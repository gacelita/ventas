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
   [ventas.utils :as utils]))

(defn is-entity? [entity]
  (when (map? entity)
    (contains? (set (keys entity)) :schema/type)))

(spec/def ::entity-type
  (spec/keys :opt-un [::attributes
                      ::to-json
                      ::filter-seed
                      ::filter-create
                      ::filter-update
                      ::before-seed
                      ::before-create
                      ::before-delete
                      ::after-seed
                      ::after-create
                      ::after-delete]))

(defonce registered-types (atom {}))

(defn register-type!
  "Registers an entity type
   Example: (register-entity-type! :user)"
  [kw & [m]]
  {:pre [(keyword? kw) (or (nil? m)
                           (and (map? m) (utils/check ::entity-type m)))]}
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

(defn type-properties
  "Returns the functions of an entity type"
  [type]
  (get @registered-types type))

(declare default-type)

(defn- call-type-fn [kw entity & args]
  (let [type-properties (type-properties (type entity))
        type-fn (if (kw type-properties)
                   (kw type-properties)
                   (kw default-type))]
    (apply type-fn entity args)))

(defn to-json
  "Transforms an entity into a stripped map, suitable for sending to the outside"
  [entity & [options]]
  {:pre [(is-entity? entity)]}
  (call-type-fn :to-json entity options))

(defn filter-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-seed entity))

(defn filter-create [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :filter-create entity))

(defn filter-update [entity data]
  {:pre [(is-entity? entity) (map? data)]}
  (call-type-fn :filter-update entity data))

(defn before-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-seed entity))

(defn before-create [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-create entity))

(defn before-delete [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :before-delete entity))

(defn after-seed [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-seed entity))

(defn after-create [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-create entity))

(defn after-delete [entity]
  {:pre [(is-entity? entity)]}
  (call-type-fn :after-delete entity))

(defn spec
  "Checks that an entity complies with its spec"
  [entity]
  {:pre [(type-exists? (type entity))]}
  (let [spec (:schema/type entity)]
    (when (utils/spec-exists? spec)
      (utils/check spec entity))))

(defn find
  "Finds an entity by eid or lookup ref"
  [eid]
  {:pre [(or (db/lookup-ref? eid) (number? eid))]}
  (db/touch-eid eid))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (-> (db/resolve-tempid (:tempids tx) tempid)
      (find)))

(defn- prepare-creation-attrs [pre-entity & [initial-tempid]]
  (into {}
        (map (fn [[k v]]
               [k (cond
                    (is-entity? v) (prepare-creation-attrs v)
                    (and (coll? v) (is-entity? (first v))) (map prepare-creation-attrs v)
                    :else v)])
             (-> pre-entity
                 (assoc :db/id (or initial-tempid (d/tempid :db.part/user)))
                 (filter-create)))))

(defn create*
  "Creates an entity"
  [attrs]
  {:pre [(do (debug attrs) (spec attrs))]}
  (before-create attrs)
  (let [tempid (d/tempid :db.part/user)
        pre-entity (prepare-creation-attrs attrs tempid)
        tx (db/transact [pre-entity])
        entity (transaction->entity tx tempid)]
    (after-create entity)
    entity))

(defn create
  "Creates an entity from unqualified attributes.
   Example usage:
   (create :user {:name `Joel` :email `test@test.com`})"
  [type attributes]
  (let [entity
        (-> (utils/qualify-map-keywords (utils/filter-empty-vals attributes) type)
            (assoc :schema/type (db/kw->type type)))]
    (create* entity)))

(defn from-json
  "Inverse of to-json"
  [attributes]
  {:pre [(map? attributes)]}
  (let [id (:id attributes)
        type (:schema/type (find id))]
    (-> attributes
        (dissoc :id)
        (dissoc :type)
        (utils/qualify-map-keywords type)
        (assoc :schema/type type)
        (assoc :db/id id))))

(defn attributes
  [type]
  (:attributes (type-properties type)))

(defn fixtures
  [type]
  (when-let [fixtures-fn (:fixtures (type-properties type))]
    (->> (fixtures-fn)
         (map #(assoc % :schema/type (db/kw->type type))))))

(defn autoresolve?
  [type]
  (:autoresolve? (type-properties type)))

(defn dependencies
  [type]
  (or (:dependencies (type-properties type)) #{}))

(defn seed-number
  [type]
  (or (:seed-number (type-properties type)) 30))

(spec/def
  ::ref
  (spec/or :eid number?
           :entity is-entity?
           :lookup-ref db/lookup-ref?))

(spec/def
  ::refs
  (spec/coll-of ::ref))

(defn attributes-by-ident
  [type]
  "Returns a map whose keys are idents and whose values are the properties of each ident"
  (into {}
        (map
         (fn [attr]
           [(:db/ident attr) attr])
         (attributes type))))

(defn idents-with-value-type
  "Returns the idents of an entity type with the given valueType"
  [type value-type]
  (let [attrs (attributes-by-ident type)]
    (->> attrs
         (filter (fn [[k v]]
                   (= value-type (:db/valueType v))))
         (into {})
         (keys)
         (set))))

(defn- autoresolve-ref [ref & [options]]
  (let [subentity (-> ref find)]
    (if (autoresolve? (db/type->kw (:schema/type subentity)))
      (to-json subentity options)
      ref)))

(defn autoresolve
  "Resolves references to entity types that have an :autoresolve?
   property with a truthy value"
  [entity & [options]]
  (let [ref-idents (idents-with-value-type (db/type->kw (:schema/type entity))
                                           :db.type/ref)]
    (->> entity
         (map (fn [[ident value]]
                [ident (if-not (contains? ref-idents ident)
                         value
                         (cond
                           (spec/valid? (spec/coll-of number?) value)
                             (map #(autoresolve-ref % options) value)
                           (spec/valid? number? value)
                             (autoresolve-ref value options)
                           :else value))]))
         (into {}))))

(defn default-to-json [entity & [options]]
  (-> (autoresolve entity options)
      (dissoc :schema/type)
      (utils/dequalify-keywords)))

(def ^:private default-type
  {:attributes []
   :to-json default-to-json
   :filter-seed identity
   :filter-create identity
   :filter-update (fn [entity data] data)
   :before-seed (fn [_] true)
   :before-create (fn [_] true)
   :before-delete (fn [_] true)
   :after-seed (fn [_] true)
   :after-create (fn [_] true)
   :after-delete (fn [_] true)})

(defn default-attr [attr-name]
  (get default-type attr-name))

(defn- get-enum-retractions [entity new-values]
  (let [relevant-attrs (filter #(contains? (set (keys new-values)) (:db/ident %))
                               (attributes (type entity)))
        enum-attrs (filter #(and (= (:db/cardinality %) :db.cardinality/many)
                                 (= (:db/valueType %) :db.type/ref))
                           relevant-attrs)
        enum-idents (map :db/ident enum-attrs)]
    (mapcat (fn [{:keys [ident new-val]}]
              (let [diff (set/difference (set (get entity ident))
                                         (set new-val))]
                (map (fn [val-to-retract]
                       [:db/retract (:db/id new-values) ident val-to-retract])
                     diff)))
            (map #(hash-map :ident % :new-val (get new-values %))
                 enum-idents))))

(defn update*
  "Updates an entity.
   Example usage:
   (update {:db/id 1234567
            :user/name `Other name`})"
  [{:db/keys [id] :as attrs}]
  (let [entity (find id)]
    (filter-update entity attrs)
    (db/transact (concat [attrs] (get-enum-retractions entity attrs)))
    (find id)))

(defn update
  "Updates an entity from unqualified attributes.
   Example usage:
   (update {:id 1234567 :name `Other name`})"
  [{:keys [id] :as attrs}]
  {:pre [(map? attrs) id]}
  (let [entity (find id)]
    (update* (-> attrs
                 (dissoc :id)
                 (utils/qualify-map-keywords (type entity))
                 (assoc :db/id (:db/id entity))))))

(defn delete [eid]
  "Deletes an entity by eid"
  (let [entity (find eid)]
    (before-delete entity)
    (db/retract-entity eid)
    (after-delete entity)))

(defn upsert
  [type {:keys [id] :as attributes}]
  {:pre [(keyword? type) (map? attributes)]}
  (if id
    (update attributes)
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

(defn- filters->wheres* [type filters]
  (->> filters
       (map (fn [[attribute value]]
              (let [attribute (if (namespace attribute)
                                attribute
                                (utils/qualify-keyword attribute type))
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

(defn filters->wheres
  "Generates `:where` clauses"
  [type filters]
  (filters->wheres*
   type
   (as-> filters filters
         (filter (fn [[k v]]
                   (or (and (not (coll? v)) v)
                       (seq v)))
                 filters)
         (if (empty? filters)
           {:schema/type (db/kw->type type)}
           filters))))

(defn query
  "Performs a high-level query.
   Accepts optional `wheres` clauses"
  [type & [filters]]
  (map #(find (:id %))
       (db/nice-query {:find '[?id]
                       :where (filters->wheres type filters)})))

(defn generate*
  "Generates one sample of a given entity type"
  [type]
  (let [db-type (db/kw->type type)]
    (-> (gen/generate (spec/gen db-type))
        (assoc :schema/type db-type))))

(defn generate
  "Generates n samples of a given entity type"
  [type & [n]]
  (let [n (or n 1)]
    (map generate* (repeat n type))))

(defn ref-generator [type & {:keys [new?]}]
  (gen/elements
   (if new?
     (generate type)
     (map :db/id (query type)))))

(defn refs-generator [type & {:keys [new?]}]
  (gen/vector (ref-generator type :new? new?)))
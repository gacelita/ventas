(ns ventas.database.entity
  (:refer-clojure :exclude [find type update])
  (:require
   [clojure.core :as clj]
   [clojure.core.async :refer [<! go go-loop]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [datomic.api :as d]
   [slingshot.slingshot :refer [throw+]]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.generators :as db.generators]
   [ventas.database.schema :as schema]
   [ventas.utils :as utils]))

(defn entity? [entity]
  (when (map? entity)
    (spec/valid? ::entity (select-keys entity #{:schema/type}))))

(spec/def
  ::ref
  (spec/or :eid number?
           :entity entity?
           :lookup-ref db/lookup-ref?
           :pull-eid ::db/pull-eid))

(spec/def
  ::refs
  (spec/coll-of ::ref))

(spec/def :schema/type
  (spec/or :pull-eid ::db/pull-eid
           :keyword ::db.generators/keyword))

(spec/def ::entity
  (spec/keys :req [:schema/type]))

(spec/def ::attributes sequential?)

(spec/def ::entity-type
  (spec/keys :req-un [::attributes]))

(defn kw->type [kw]
  {:pre [(keyword? kw)]}
  (keyword "schema.type" (name kw)))

(defn type->kw [type]
  {:pre [(keyword? type)]}
  (keyword (name type)))

(defn db-migrated? []
  (and (db/connected?) (db/entity :schema/type)))

(defn check-db-migrated!
  "Throws if (not (db-migrated?))"
  []
  (when-not (db-migrated?)
    (throw+ {:type ::database-not-migrated
             :message "The database needs to be migrated"})))

(defonce ^:private registered-types (atom {}))

(defn register-type!
  "Registers an entity type
   Example: (register-entity-type! :user)"
  [kw & [m]]
  {:pre [(keyword? kw) (not (namespace kw)) (or (nil? m) (map? m))]}
  (let [m (or m {})]
    (schema/register-migration!
     (keyword (name kw) "entity-type-ident")
     [{:db/ident (kw->type kw)}])
    (doseq [[key attributes] (:migrations m)]
      (schema/register-migration!
       (keyword (name kw) (name key))
       attributes))
    (swap! registered-types assoc kw m))
  :done)

(defn types
  "Returns all types"
  []
  @registered-types)

(defn type-exists?
  [type]
  {:pre [(keyword? type)]}
  (some? (get @registered-types type)))

(defn type
  "Returns the type of an entity"
  [entity]
  {:pre [(entity? entity)]}
  (let [type (:schema/type entity)]
    (cond
      (keyword? type) (keyword (name type))
      (:db/id type)   (db/ident (:db/id type)))))

(defn type-properties
  "Returns the properties of an entity type"
  [type]
  (get @registered-types type))

(declare default-type)

(defn type-property [entity-type property]
  (or (get (type-properties entity-type) property)
      (property default-type)))

(defn call-type-fn [property entity & args]
  (let [type-fn (type-property (type entity) property)]
    (apply type-fn entity args)))

(defn serialize
  "Transforms an entity into a stripped map, suitable for sending to the outside"
  [entity & [options]]
  {:pre [(entity? entity)]}
  (call-type-fn :serialize entity options))

(defn deserialize
  "The inverse of serialize"
  [type data]
  (let [type-fn (type-property type :deserialize)]
    (type-fn data)))

(defn filter-query [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :filter-query entity))

(defn filter-seed [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :filter-seed entity))

(defn filter-create [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :filter-create entity))

(defn filter-update [entity data]
  {:pre [(entity? entity) (map? data)]}
  (call-type-fn :filter-update entity data))

(defn before-seed [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :before-seed entity))

(defn before-create [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :before-create entity))

(defn before-delete [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :before-delete entity))

(defn before-update [entity new-attrs]
  {:pre [(entity? entity)]}
  (call-type-fn :before-update entity new-attrs))

(defn after-seed [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-seed entity))

(defn after-create [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-create entity))

(defn after-delete [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-delete entity))

(defn after-update
  [entity new-entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-update entity new-entity))

(defn spec!
  "Checks that an entity complies with its spec"
  [entity]
  {:pre [(type-exists? (type entity))]}
  (let [spec (:schema/type entity)]
    (when (utils/spec-exists? spec)
      (utils/check spec entity))))

(defn find
  "Finds an entity by eid or lookup ref
   Returns nil if no entity was found"
  [eid]
  {:pre [(or (db/lookup-ref? eid) (number? eid) (keyword? eid))]}
  (let [result (db/touch-eid eid)]
    (when (entity? result)
      (filter-query result))))

(defn find-serialize
  "Shortcut for (serialize (find eid) params)"
  [eid & [params]]
  (when-let [entity (find eid)]
    (serialize entity params)))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (find (db/resolve-tempid (:tempids tx) tempid)))

(defn- prepare-creation-attrs [pre-entity & [initial-tempid]]
  (utils/mapm (fn [[k v]]
                [k (cond
                     (entity? v) (prepare-creation-attrs v)
                     (and (coll? v) (entity? (first v))) (map prepare-creation-attrs v)
                     :else v)])
              (-> pre-entity
                  (clj/update :db/id #(or % initial-tempid (db/tempid)))
                  (filter-create))))

(defn lifecycle-create [attrs transact-fn]
  (spec! attrs)
  (before-create attrs)
  (let [tempid (db/tempid)
        pre-entity (prepare-creation-attrs attrs tempid)
        tx (transact-fn [pre-entity
                         {:db/id (d/tempid :db.part/tx)
                          :event/kind :entity.create}])
        entity (transaction->entity tx tempid)]
    (after-create entity)
    entity))

(defn create*
  "Creates an entity"
  [attrs]
  (check-db-migrated!)
  (lifecycle-create attrs db/transact))

(defn create
  "Creates an entity from unqualified attributes.
   Example usage:
   (create :user {:name `Joel` :email `test@test.com`})"
  [type attributes]
  (create* (-> attributes
               common.utils/remove-nil-vals
               (utils/qualify-map-keywords type)
               (assoc :schema/type (kw->type type)))))

(defn lifecycle-seed [attrs transact-fn]
  (let [attrs (filter-seed attrs)
        _ (before-seed attrs)
        entity (lifecycle-create attrs transact-fn)]
    (after-seed entity)))

(defn seed*
  [attrs]
  (check-db-migrated!)
  (lifecycle-seed attrs db/transact))

(defn fixtures
  [type]
  (when-let [fixtures-fn (type-property type :fixtures)]
    (map (fn [fixture]
           (clj/update fixture :schema/type #(or % (kw->type type))))
         (fixtures-fn))))

(defn autoresolve?
  [type]
  (type-property type :autoresolve?))

(defn component?
  [type]
  (type-property type :component?))

(defn dependencies
  [type]
  (or (type-property type :dependencies) #{}))

(defn seed-number
  [type]
  (or (type-property type :seed-number) 30))

(defn idents-with-value-type
  "Returns the idents of an entity with the given valueType"
  [entity value-type]
  (->> (keys entity)
       (map db/touch-eid)
       (filter (fn [v]
                 (= value-type (:db/valueType v))))
       (map :db/ident)
       (set)))

(defn- autoresolve-ref [ref & [options]]
  (let [subentity (find ref)]
    (if (and subentity (autoresolve? (type->kw (:schema/type subentity))))
      (serialize subentity options)
      ref)))

(defn- autoresolve
  "Resolves references to entity types that have an :autoresolve?
   property with a truthy value"
  [entity & [options]]
  (let [ref-idents (idents-with-value-type entity :db.type/ref)]
    (utils/mapm (fn [[ident value]]
                  [ident (if-not (contains? ref-idents ident)
                           value
                           (cond
                             (spec/valid? (spec/coll-of number?) value)
                             (map #(autoresolve-ref % options) value)

                             (spec/valid? number? value)
                             (autoresolve-ref value options)

                             :else value))])
                entity)))

(defn default-serialize [entity & [{:keys [keep-type?] :as options}]]
  (let [result (utils/dequalify-keywords (autoresolve entity options))]
    (if keep-type?
      (clojure.core/update result :type #(keyword (name %)))
      (dissoc result :type))))

(defn- unresolve* [attr]
  (if (sequential? attr)
    (map unresolve* attr)
    (if (and (map? attr) (:id attr))
      (:id attr)
      attr)))

(defn- unresolve [attrs]
  (common.utils/map-vals unresolve* attrs))

(defn default-deserialize
  "Inverse of serialize"
  [attributes]
  {:pre [(map? attributes)]}
  (let [id (:id attributes)
        type (:schema/type (find id))]
    (-> attributes
        (unresolve)
        (dissoc :id)
        (dissoc :type)
        (utils/qualify-map-keywords type)
        (assoc :schema/type type)
        (assoc :db/id id))))

(def ^:private default-type
  {:attributes []
   :serialize default-serialize
   :deserialize default-deserialize
   :filter-query identity
   :filter-seed identity
   :filter-create identity
   :filter-update (fn [_ data] data)
   :before-seed (fn [_] true)
   :before-create (fn [_] true)
   :before-delete (fn [_] true)
   :before-update (fn [_ _] true)
   :after-seed (fn [_] true)
   :after-create (fn [_] true)
   :after-delete (fn [_] true)
   :after-update (fn [_ _] true)})

(defn default-attr [attr-name]
  (get default-type attr-name))

(defn- normalize-refs [refs]
  (when-not (map? refs)
    (map db/normalize-ref refs)))

(defn- enum-retractions
  "Retracts no longer present enum values:
   (enum-retractions {:db/id 17592186045691
                      :user/favorites [17592186045648 17592186045679]}
                     {:db/id 17592186045691
                      :user/favorites [17592186045648]})
   => ([:db/retract 17592186045691 :user/favorites 17592186045679])"
  [old-values new-values]
  (let [ident->values-set (fn [ident values]
                            (->> (get values ident)
                                 (normalize-refs)
                                 (set)
                                 (not-empty)))]
    (->> (set (keys new-values))
         (map db/touch-eid)
         (filter #(and (= (:db/cardinality %) :db.cardinality/many)
                       (= (:db/valueType %) :db.type/ref)))
         (map :db/ident)
         (mapcat (fn [ident]
                   (let [new-vals (ident->values-set ident new-values)
                         old-vals (ident->values-set ident old-values)
                         diff (set/difference old-vals new-vals)]
                     (map (fn [val-to-retract]
                            [:db/retract (:db/id new-values) ident val-to-retract])
                          diff)))))))

(defn lifecycle-update [{:db/keys [id] :as attrs} {:keys [append?]} transact-fn]
  (let [entity (find id)
        attrs (filter-update entity attrs)]
    (before-update entity attrs)
    (transact-fn (utils/into-n
                  [attrs]
                  (when-not append?
                    (enum-retractions entity attrs))
                  [{:db/id (d/tempid :db.part/tx)
                    :event/kind :entity.update}]))
    (let [result (find id)]
      (after-update entity result)
      result)))

(defn update*
  "Updates an entity.
   Example usage:
   (update* {:db/id 1234567
             :user/first-name `Other name`})"
  [attrs & opts]
  (check-db-migrated!)
  (lifecycle-update attrs opts db/transact))

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
    (db/transact [[:db.fn/retractEntity eid]
                  {:db/id (d/tempid :db.part/tx)
                   :event/kind :entity.delete}])
    (after-delete entity)))

(defn upsert
  [type {:keys [id] :as attributes}]
  {:pre [(keyword? type) (map? attributes)]}
  (if id
    (update attributes)
    (create type attributes)))

(defn upsert*
  [{:db/keys [id] :as attributes}]
  {:pre [(map? attributes)]}
  (if id
    (update* attributes)
    (create* attributes)))

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
       ;; autoqualify attributes, handle :any
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
           {:schema/type (kw->type type)}
           filters))))

(defn query
  "Performs a high-level query.
   Accepts optional `wheres` clauses"
  [type & [filters]]
  (check-db-migrated!)
  (map #(find (:id %))
       (db/nice-query {:find '[?id]
                       :where (filters->wheres type filters)})))

(defn query-one
  "(first (query))"
  [type & [filters]]
  (first (query type filters)))

(defn generate*
  "Generates one sample of a given entity type"
  [type]
  (let [db-type (kw->type type)]
    (-> (spec/gen db-type)
        (gen/generate)
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
     (let [entities (query type)]
       (if (seq entities)
         (map :db/id entities)
         (generate type))))))

(defn refs-generator [type & {:keys [new?]}]
  (gen/vector (ref-generator type :new? new?)))

(defn find-recursively [eid]
  "Finds the given eid or lookup ref, and all the refs inside it"
  (let [entity (find eid)
        ref-idents (idents-with-value-type entity :db.type/ref)]
    (utils/mapm (fn [[k v]]
                  [k (if (and (contains? ref-idents k) (not (keyword? v)))
                       (if (sequential? v)
                         (map find-recursively v)
                         (find-recursively v))
                       v)])
                entity)))

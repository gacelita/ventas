(ns ventas.database.entity
  (:refer-clojure :exclude [find type update])
  (:require
   [clojure.core.async :refer [<! go go-loop]]
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [datomic.api :as d]
   [slingshot.slingshot :refer [throw+]]
   [taoensso.timbre :as timbre]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.generators :as db.generators]
   [ventas.database.schema :as schema]
   [ventas.utils :as utils]))

(spec/def :schema/type
  (spec/or :pull-eid ::db/pull-eid
           :keyword ::db.generators/keyword))

(spec/def ::entity
  (spec/keys :req [:schema/type]))

(spec/def ::entity-type
  (spec/keys :opt-un [::attributes
                      ::serialize
                      ::filter-seed
                      ::filter-create
                      ::filter-update
                      ::before-seed
                      ::before-create
                      ::before-delete
                      ::after-seed
                      ::after-create
                      ::after-delete]))

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
             :message "The database needs to be migrated before doing this"})))

(defn entity? [entity]
  (when (map? entity)
    (spec/valid? ::entity (select-keys entity #{:schema/type}))))

(defonce registered-types (atom {}))

(defn register-type!
  "Registers an entity type
   Example: (register-entity-type! :user)"
  [kw & [m]]
  {:pre [(keyword? kw) (or (nil? m)
                           (and (map? m) (utils/check ::entity-type m)))]}
  (let [m (or m {})]
    (schema/register-migration!
     [{:db/ident (kw->type kw)}])
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
  {:pre [(entity? entity)]}
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

(defn serialize
  "Transforms an entity into a stripped map, suitable for sending to the outside"
  [entity & [options]]
  {:pre [(entity? entity)]}
  (call-type-fn :serialize entity options))

(defn deserialize
  [type data]
  (let [type-properties (type-properties type)
        type-fn (if-let [f (:deserialize type-properties)]
                  f
                  (:deserialize default-type))]
    (type-fn data)))

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

(defn after-seed [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-seed entity))

(defn after-create [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-create entity))

(defn after-delete [entity]
  {:pre [(entity? entity)]}
  (call-type-fn :after-delete entity))

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
  {:pre [(or (db/lookup-ref? eid) (number? eid))]}
  (let [result (db/touch-eid eid)]
    (when (entity? result)
      result)))

(defn resolve-by-slug
  [slug]
  (db/nice-query-attr
   {:find '[?id]
    :in {'?slug slug}
    :where '[[?translation :i18n.translation/value ?slug]
             [?i18n :i18n/translations ?translation]
             [?id :ventas/slug ?i18n]]}))

(defn find-by-slug
  [slug]
  (-> (resolve-by-slug slug)
      (find)))

(defn find-serialize
  "Same as doing (serialize (find eid) params), which is a very common thing to do"
  [eid & [params]]
  (when-let [entity (find eid)]
    (serialize entity params)))

(defn transaction->entity
  "Returns an entity from a transaction"
  [tx tempid]
  (-> (db/resolve-tempid (:tempids tx) tempid)
      (find)))

(defn- prepare-creation-attrs [pre-entity & [initial-tempid]]
  (into {}
        (map (fn [[k v]]
               [k (cond
                    (entity? v) (prepare-creation-attrs v)
                    (and (coll? v) (entity? (first v))) (map prepare-creation-attrs v)
                    :else v)])
             (-> pre-entity
                 (assoc :db/id (or initial-tempid (db/tempid)))
                 (filter-create)))))

(defn create*
  "Creates an entity"
  [attrs]
  (timbre/debug attrs)
  (spec! attrs)
  (check-db-migrated!)
  (before-create attrs)
  (let [tempid (db/tempid)
        pre-entity (prepare-creation-attrs attrs tempid)
        tx (db/transact [pre-entity
                         {:db/id (d/tempid :db.part/tx)
                          :event/kind :entity.create}])
        entity (transaction->entity tx tempid)]
    (after-create entity)
    entity))

(defn create
  "Creates an entity from unqualified attributes.
   Example usage:
   (create :user {:name `Joel` :email `test@test.com`})"
  [type attributes]
  (let [entity
        (-> (utils/qualify-map-keywords (common.utils/remove-nil-vals attributes) type)
            (assoc :schema/type (kw->type type)))]
    (create* entity)))

(defn attributes
  [type]
  (:attributes (type-properties type)))

(defn fixtures
  [type]
  (when-let [fixtures-fn (:fixtures (type-properties type))]
    (->> (fixtures-fn)
         (map #(assoc % :schema/type (kw->type type))))))

(defn autoresolve?
  [type]
  (:autoresolve? (type-properties type)))

(defn component?
  [type]
  (:component? (type-properties type)))

(defn dependencies
  [type]
  (or (:dependencies (type-properties type)) #{}))

(defn seed-number
  [type]
  (or (:seed-number (type-properties type)) 30))

(spec/def
  ::ref
  (spec/or :eid number?
           :entity entity?
           :lookup-ref db/lookup-ref?
           :pull-eid ::db/pull-eid))

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
  "Returns the idents of an entity with the given valueType"
  [entity value-type]
  (->> (keys entity)
       (map db/touch-eid)
       (filter (fn [v]
                 (= value-type (:db/valueType v))))
       (map :db/ident)
       (set)))

(defn- autoresolve-ref [ref & [options]]
  (let [subentity (-> ref find)]
    (if (and subentity (autoresolve? (type->kw (:schema/type subentity))))
      (serialize subentity options)
      ref)))

(defn- autoresolve
  "Resolves references to entity types that have an :autoresolve?
   property with a truthy value"
  [entity & [options]]
  (let [ref-idents (idents-with-value-type entity :db.type/ref)]
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

(defn default-serialize [entity & [{:keys [keep-type?] :as options}]]
  (let [result (-> (autoresolve entity options)
                   (utils/dequalify-keywords))]
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

(defn- normalize-refs [refs]
  (when-not (map? refs)
    (map db/normalize-ref refs)))

(defn- get-enum-retractions
  [old-values new-values]
  (let [relevant-attrs (filter #(contains? (set (keys new-values)) (:db/ident %))
                               (attributes (type old-values)))
        enum-attrs (filter #(and (= (:db/cardinality %) :db.cardinality/many)
                                 (= (:db/valueType %) :db.type/ref))
                           relevant-attrs)
        enum-idents (map :db/ident enum-attrs)
        enum-vals (->> (map (fn [ident]
                              [ident (->> (get old-values ident)
                                          (map db/normalize-ref)
                                          (set))])
                            enum-idents)
                       (into {}))]
    (mapcat (fn [{:keys [ident new-val]}]
              (let [diff (set/difference (get enum-vals ident)
                                         new-val)]
                (map (fn [val-to-retract]
                       [:db/retract (:db/id new-values) ident val-to-retract])
                     diff)))
            (map #(hash-map :ident % :new-val (->> (get new-values %)
                                                   normalize-refs
                                                   (set)))
                 enum-idents))))

(defn update*
  "Updates an entity.
   Example usage:
   (update* {:db/id 1234567
             :user/first-name `Other name`})"
  [{:db/keys [id] :as attrs} & {:keys [append?]}]
  (check-db-migrated!)
  (let [entity (find id)
        attrs (filter-update entity attrs)]
    (db/transact (concat [attrs]
                         (when-not append?
                           (get-enum-retractions entity attrs))
                         [{:db/id (d/tempid :db.part/tx)
                           :event/kind :entity.update}]))
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
    (->> entity
         (map (fn [[k v]]
                [k (if (and (contains? ref-idents k) (not (keyword? v)))
                     (if (sequential? v)
                       (map find-recursively v)
                       (find-recursively v))
                     v)]))
         (into {}))))

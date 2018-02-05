(ns ventas.server.api.description
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [clojure.test.check.generators :as gen]
   [spec-tools.core :as st]
   [spec-tools.data-spec :as data-spec]
   [spec-tools.impl :as impl]
   [spec-tools.parse :as parse]
   [spec-tools.visitor :as visitor]
   [ventas.common.utils :as common.utils]
   [ventas.server.api :as api]))

(defn- only-entry? [key a-map] (= [key] (keys a-map)))

(defn- simplify-all-of [spec]
  (let [subspecs (->> (:all-of spec) (remove empty?))]
    (cond
      (empty? subspecs) (dissoc spec :all-of)
      (and (= (count subspecs) 1) (only-entry? :all-of spec)) (first subspecs)
      :else (assoc spec :all-of subspecs))))

(defn- spec-dispatch [dispatch _ _ _] dispatch)

(defmulti accept-spec spec-dispatch :default ::default)

(defn transform
  ([spec]
   (transform spec nil))
  ([spec options]
   (visitor/visit spec accept-spec options)))

; any? (one-of [(return nil) (any-printable)])
(defmethod accept-spec 'clojure.core/any? [_ _ _ _] {})

; some? (such-that some? (any-printable))
(defmethod accept-spec 'clojure.core/some? [_ _ _ _] {})

; number? (one-of [(large-integer) (double)])
(defmethod accept-spec 'clojure.core/number? [_ _ _ _] {:type :number})

(defmethod accept-spec 'clojure.core/pos? [_ _ _ _] {:minimum 0 :exclusive-minimum true})

(defmethod accept-spec 'clojure.core/neg? [_ _ _ _] {:maximum 0 :exclusive-maximum true})

; integer? (large-integer)
(defmethod accept-spec 'clojure.core/integer? [_ _ _ _] {:type :number})

; int? (large-integer)
(defmethod accept-spec 'clojure.core/int? [_ _ _ _] {:type :number})

; pos-int? (large-integer* {:min 1})
(defmethod accept-spec 'clojure.core/pos-int? [_ _ _ _] {:type :number :minimum 1})

; neg-int? (large-integer* {:max -1})
(defmethod accept-spec 'clojure.core/neg-int? [_ _ _ _] {:type :number :maximum -1})

; nat-int? (large-integer* {:min 0})
(defmethod accept-spec 'clojure.core/nat-int? [_ _ _ _] {:type :number :minimum 0})

; float? (double)
(defmethod accept-spec 'clojure.core/float? [_ _ _ _] {:type :float})

; double? (double)
(defmethod accept-spec 'clojure.core/double? [_ _ _ _] {:type :double})

; boolean? (boolean)
(defmethod accept-spec 'clojure.core/boolean? [_ _ _ _] {:type :boolean})

; string? (string-alphanumeric)
(defmethod accept-spec 'clojure.core/string? [_ _ _ _] {:type :string})

; ident? (one-of [(keyword-ns) (symbol-ns)])
(defmethod accept-spec 'clojure.core/ident? [_ _ _ _] {:type :ident})

; simple-ident? (one-of [(keyword) (symbol)])
(defmethod accept-spec 'clojure.core/simple-ident? [_ _ _ _] {:type :ident
                                                              :qualified? false})

; qualified-ident? (such-that qualified? (one-of [(keyword-ns) (symbol-ns)]))
(defmethod accept-spec 'clojure.core/qualified-ident? [_ _ _ _] {:type :ident
                                                                 :qualified? true})

; keyword? (keyword-ns)
(defmethod accept-spec 'clojure.core/keyword? [_ _ _ _] {:type :keyword})

; simple-keyword? (keyword)
(defmethod accept-spec 'clojure.core/simple-keyword? [_ _ _ _] {:type :keyword
                                                                :qualified? false})

; qualified-keyword? (such-that qualified? (keyword-ns))
(defmethod accept-spec 'clojure.core/qualified-keyword? [_ _ _ _] {:type :keyword
                                                                   :qualified? true})

; symbol? (symbol-ns)
(defmethod accept-spec 'clojure.core/symbol? [_ _ _ _] {:type :symbol})

; simple-symbol? (symbol)
(defmethod accept-spec 'clojure.core/simple-symbol? [_ _ _ _] {:type :symbol
                                                               :qualified? false})

; qualified-symbol? (such-that qualified? (symbol-ns))
(defmethod accept-spec 'clojure.core/qualified-symbol? [_ _ _ _] {:type :symbol
                                                                  :qualified? true})

; uuid? (uuid)
(defmethod accept-spec 'clojure.core/uuid? [_ _ _ _] {:type :string :format :uuid})

; uri? (fmap #(java.net.URI/create (str "http://" % ".com")) (uuid))
(defmethod accept-spec 'clojure.core/uri? [_ _ _ _] {:type :string :format :uri})

; bigdec? (fmap #(BigDecimal/valueOf %)
;               (double* {:infinite? false :NaN? false}))
(defmethod accept-spec 'clojure.core/decimal? [_ _ _ _] {:type :double})

; inst? (fmap #(java.util.Date. %)
;             (large-integer))
(defmethod accept-spec 'clojure.core/inst? [_ _ _ _] {:type :string :format :date-time})

; seqable? (one-of [(return nil)
;                   (list simple)
;                   (vector simple)
;                   (map simple simple)
;                   (set simple)
;                   (string-alphanumeric)])
(defmethod accept-spec 'clojure.core/seqable? [_ _ _ _] {:type :seqable})

; indexed? (vector simple)
(defmethod accept-spec 'clojure.core/map? [_ _ _ _] {:type :map})

; vector? (vector simple)
(defmethod accept-spec 'clojure.core/vector? [_ _ _ _] {:type :vector})

; list? (list simple)
(defmethod accept-spec 'clojure.core/list? [_ _ _ _] {:type :list})

; seq? (list simple)
(defmethod accept-spec 'clojure.core/seq? [_ _ _ _] {:type :seq})

; char? (char)
(defmethod accept-spec 'clojure.core/char? [_ _ _ _] {:type :char})

; set? (set simple)
(defmethod accept-spec 'clojure.core/set? [_ _ _ _] {:type :set})

; nil? (return nil)
(defmethod accept-spec 'clojure.core/nil? [_ _ _ _] {:type :nil})

(defmethod accept-spec 'clojure.core/some? [_ _ _ _] {:type :any})

; false? (return false)
(defmethod accept-spec 'clojure.core/false? [_ _ _ _] {:type :boolean :value false})

; true? (return true)
(defmethod accept-spec 'clojure.core/true? [_ _ _ _] {:type :boolean :value true})

; zero? (return 0)
(defmethod accept-spec 'clojure.core/zero? [_ _ _ _] {:type :number :value 0})

; coll? (one-of [(map simple simple)
;                (list simple)
;                (vector simple)
;                (set simple)])
(defmethod accept-spec 'clojure.core/coll? [_ _ _ _] {:type :coll})

; empty? (elements [nil '() [] {} #{}])
(defmethod accept-spec 'clojure.core/empty? [_ _ _ _] {:type :seq :max-items 0 :min-items 0})

; associative? (one-of [(map simple simple) (vector simple)])
(defmethod accept-spec 'clojure.core/associative? [_ _ _ _] {:type :associative})

; sequential? (one-of [(list simple) (vector simple)])
(defmethod accept-spec 'clojure.core/sequential? [_ _ _ _] {:type :sequential})

; ratio? (such-that ratio? (ratio))
(defmethod accept-spec 'clojure.core/ratio? [_ _ _ _] {:type :ratio})

(defmethod accept-spec ::visitor/set [dispatch spec children _]
  {:enum children})

(defn- maybe-with-title [schema spec]
  (if-let [title (st/spec-name spec)]
    (assoc schema :title (impl/qualified-name title))
    schema))

(defmethod accept-spec 'clojure.spec.alpha/keys [_ spec children _]
  (let [{:keys [req req-un opt opt-un]} (impl/parse-keys (impl/extract-form spec))
        names-un (concat req-un opt-un)
        names (map impl/qualified-name (concat req opt))
        required (map impl/qualified-name req)
        all-required (not-empty (concat required req-un))]
    (maybe-with-title
     (merge
      {:type :map
       :keys (zipmap (concat names names-un) children)}
      (when all-required
        {:required (vec all-required)}))
     spec)))

(defmethod accept-spec 'clojure.spec.alpha/or [_ _ children _]
  {:any-of children})

(defmethod accept-spec 'clojure.spec.alpha/and [_ _ children _]
  (simplify-all-of {:all-of children}))

(defmethod accept-spec 'clojure.spec.alpha/merge [_ _ children _]
  {:type :map
   :keys (apply merge (map :keys children))
   :required (into [] (reduce into (sorted-set) (map :required children)))})

(defmethod accept-spec 'clojure.spec.alpha/every [_ spec children _]
  (let [form (impl/extract-form spec)
        {:keys [type]} (parse/parse-spec form)]
    (case type
      :map (maybe-with-title {:type :map, :additional-properties (impl/unwrap children)} spec)
      :set {:type :set, :uniqueItems true, :items (impl/unwrap children)}
      :vector {:type :vector, :items (impl/unwrap children)})))

(defmethod accept-spec 'clojure.spec.alpha/every-kv [_ spec children _]
  (maybe-with-title {:type :map, :additional-properties (second children)} spec))

(defmethod accept-spec ::visitor/map-of [_ spec children _]
  (maybe-with-title {:type :map, :additional-properties (second children)} spec))

(defmethod accept-spec ::visitor/set-of [_ _ children _]
  {:type :set, :items (impl/unwrap children), :uniqueItems true})

(defmethod accept-spec ::visitor/vector-of [_ _ children _]
  {:type :vector, :items (impl/unwrap children)})

(defmethod accept-spec 'clojure.spec.alpha/* [_ _ children _]
  {:type :sequential :items (impl/unwrap children)})

(defmethod accept-spec 'clojure.spec.alpha/+ [_ _ children _]
  {:type :sequential :items (impl/unwrap children) :minItems 1})

(defmethod accept-spec 'clojure.spec.alpha/? [_ _ children _]
  {:type :sequential :items (impl/unwrap children) :minItems 0})

(defmethod accept-spec 'clojure.spec.alpha/alt [_ _ children _]
  {:any-of children})

(defmethod accept-spec 'clojure.spec.alpha/cat [_ _ children _]
  {:type :sequential
   :items {:any-of children}})

; &

(defmethod accept-spec 'clojure.spec.alpha/tuple [_ _ children _]
  {:type :sequential
   :items children})

; keys*

(defmethod accept-spec 'clojure.spec.alpha/nilable [_ _ children _]
  (assoc (impl/unwrap children)
         :nilable? true))

;; this is just a function in clojure.spec?
(defmethod accept-spec 'clojure.spec.alpha/int-in-range? [_ spec _ _]
  (let [[_ minimum maximum _] (impl/strip-fn-if-needed spec)]
    {:minimum minimum :maximum maximum}))

(defmethod accept-spec ::visitor/spec [_ spec children _]
  (let [[_ data] (impl/extract-form spec)
        json-schema-meta (reduce-kv
                          (fn [acc k v]
                            (if (= "json-schema" (namespace k))
                              (assoc acc (keyword (name k)) v)
                              acc))
                          {}
                          (into {} data))
        extra-info (-> data
                       (select-keys [:name :description])
                       (set/rename-keys {:name :title}))]
    (merge (impl/unwrap children) extra-info json-schema-meta)))

(defmethod accept-spec ::default [_ _ _ _]
  {:type :unknown})

(defn describe-api []
  (->> @api/available-requests
       (remove (fn [[request {:keys [binary?]}]]
                 binary?))
       (map (fn [[request {:keys [spec doc]}]]
              [request {:spec (when spec
                                (->> (data-spec/spec request spec)
                                     (transform)))
                        :doc doc}]))
       (into {})))

(api/register-endpoint!
 :api.describe
 (fn [_ _]
   (describe-api)))

(api/register-endpoint!
 :api.generate-params
 {:spec {:request ::api/keyword}}
 (fn [{{:keys [request]} :params} _]
   (when-let [{:keys [spec]} (get @api/available-requests request)]
     (spec/def ::temp (data-spec/spec request spec))
     (gen/generate (spec/gen ::temp)))))

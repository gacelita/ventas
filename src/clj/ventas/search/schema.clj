(ns ventas.search.schema
  (:require
   [ventas.database.entity :as entity]
   [ventas.search :as search]
   [ventas.database :as db]
   [ventas.utils :as utils]
   [clojure.string :as str]))

(defn ident->property [ident]
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
                       (merge acc (utils/mapm (fn [culture-kw]
                                                [(ident->property (with-culture ident culture-kw))
                                                 value])
                                              culture-kws)))))
                 {}))))

(defn setup! []
  (search/create-index
   {:mappings {:doc {:properties (attributes->mapping (db/schema))}}
    :settings {:analysis {:filter {:autocomplete_filter {:type "edge_ngram"
                                                         :min_gram 1
                                                         :max_gram 20}}
                          :analyzer {:autocomplete {:type "custom"
                                                    :tokenizer "standard"
                                                    :filter ["lowercase"
                                                             "autocomplete_filter"]}}}}}))

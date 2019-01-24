(ns ventas.entities.i18n
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [slingshot.slingshot :refer [throw+]]
   [ventas.common.utils :as common.utils]
   [ventas.database :as db]
   [ventas.database.entity :as entity]
   [ventas.database.generators :as generators]
   [ventas.utils :as utils :refer [mapm]]
   [ventas.search.indexing :as search.indexing]))

(spec/def :i18n.culture/keyword ::generators/keyword)

(spec/def :i18n.culture/name ::generators/string)

(spec/def :schema.type/i18n.culture
  (spec/keys :req [:i18n.culture/keyword
                   :i18n.culture/name]))

(entity/register-type!
 :i18n.culture
 {:migrations
  [[:base [{:db/ident :i18n.culture/keyword
            :db/valueType :db.type/keyword
            :db/cardinality :db.cardinality/one
            :db/unique :db.unique/identity}
           {:db/ident :i18n.culture/name
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}]]]

  :fixtures
  ;; @TODO Remove
  (fn []
    [{:i18n.culture/keyword :en_US
      :i18n.culture/name "English (US)"}
     {:i18n.culture/keyword :es_ES
      :i18n.culture/name "Español (España)"}])

  :serialize
  (fn [this _]
    (:db/id this))

  :deserialize
  (fn [this]
    (entity/find this))

  :seed-number 0
  :autoresolve? true})

(spec/def :i18n.translation/value ::generators/string)

(spec/def :i18n.translation/culture
  (spec/with-gen ::entity/ref #(entity/ref-generator :i18n.culture)))

(spec/def :schema.type/i18n.translation
  (spec/keys :req [:i18n.translation/value
                   :i18n.translation/culture]))

(entity/register-type!
 :i18n.translation
 {:migrations
  [[:base [{:db/ident :i18n.translation/value
            :db/valueType :db.type/string
            :db/cardinality :db.cardinality/one}
           {:db/ident :i18n.translation/culture
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/one}]]]

  :dependencies
  #{:i18n.culture}

  :serialize
  (fn [this _]
    [(:i18n.translation/culture this)
     (:i18n.translation/value this)])

  :component? true})

(defn translations-generator-for-culture [culture-id]
  (->> (entity/generate :i18n.translation)
       (map #(assoc % :i18n.translation/culture culture-id))
       (gen/elements)))

(defn translations-generator []
  (let [culture-ids (map :db/id (entity/query :i18n.culture))]
    (apply gen/tuple
           (remove nil?
                   (map translations-generator-for-culture culture-ids)))))

(spec/def :i18n/translations
  (spec/with-gen ::entity/refs
    translations-generator))

(spec/def :schema.type/i18n
  (spec/keys :req [:i18n/translations]))

(defn normalize-i18n [i18n]
  "Normalizes the culture refs of the translations"
  (update i18n
          :i18n/translations
          #(map (fn [translation]
                  (update (if (number? translation) (entity/find translation) translation)
                          :i18n.translation/culture
                          db/normalize-ref))
                %)))

(defn- serialize-transacted [this & [culture]]
  (let [translations (mapm (comp entity/serialize entity/find)
                           (:i18n/translations this))]
    (if-not culture
      translations
      (or (get translations culture)
          (second (first translations))))))

(defn- serialize-literal [this & [culture]]
  (let [this (normalize-i18n this)
        serialized (mapm (fn [{:i18n.translation/keys [culture value]}]
                           [culture value])
                         (:i18n/translations this))]
    (if-not culture
      serialized
      (get serialized culture))))

(entity/register-type!
 :i18n
 {:migrations
  [[:base [{:db/ident :i18n/translations
            :db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many
            :db/isComponent true}]]]

  :dependencies
  #{:i18n.translation}

  :before-create
  (fn [{:i18n/keys [translations]}]
    (let [entities (into (->> (filter number? translations)
                              (map entity/find))
                         (filter map? translations))]
      (when (->> entities
                 (map :i18n.translation/culture)
                 (utils/has-duplicates?))
        (throw+ {:type ::duplicate-translation
                 :message "You can't add more than one translation per culture to an :i18n entity"}))))

  :serialize
  (fn [this {:keys [culture]}]
    {:pre [(or (not culture) (utils/check ::entity/ref culture))]}
    (let [culture (db/normalize-ref culture)]
      (if-not (:db/id this)
        (serialize-literal this culture)
        (serialize-transacted this culture))))

  :autoresolve? true
  :component? true})

(defmethod search.indexing/transform-entity-by-type :schema.type/i18n [entity]
  (mapm (fn [[culture value]]
          [(->> culture
                entity/find
                :i18n.culture/keyword)
           value])
        (entity/serialize entity)))

(spec/def ::ref
  (spec/with-gen ::entity/ref #(entity/ref-generator :i18n :new? true)))

(defn get-i18n-entity [translations]
  {:schema/type :schema.type/i18n
   :i18n/translations (map (fn [[culture-kw value]]
                             {:schema/type :schema.type/i18n.translation
                              :i18n.translation/value value
                              :i18n.translation/culture [:i18n.culture/keyword culture-kw]})
                           translations)})

(defn- merge-i18ns-with* [f a b]
  (let [a (normalize-i18n a)
        b (normalize-i18n b)
        b-map (->> (get b :i18n/translations)
                   (common.utils/index-by :i18n.translation/culture)
                   (common.utils/map-vals :i18n.translation/value))]
    (update a
            :i18n/translations
            (fn [translations]
              (map (fn [translation]
                     (update translation
                             :i18n.translation/value
                             #(f % (get b-map (:i18n.translation/culture translation)))))
                   translations)))))

(defn merge-i18ns-with [f & i18ns]
  (reduce (fn [acc itm]
            (merge-i18ns-with* f acc itm))
          (first i18ns)
          (rest i18ns)))

(defn culture->kw [eid]
  (:i18n.culture/keyword (entity/find eid)))
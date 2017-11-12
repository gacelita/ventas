(ns ventas.entities.i18n
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.entity :as entity]))

(spec/def :i18n.language/keyword keyword?)

(spec/def :i18n.language/name string?)

(spec/def :schema.type/i18n.language
  (spec/keys :req [:i18n.language/keyword
                   :i18n.language/name]))

(entity/register-type!
 :i18n.language
 {:attributes
  [{:db/ident :i18n.language/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :i18n.language/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]

  :fixtures
  (fn []
    [{:schema/type :schema.type/i18n.language
      :i18n.language/keyword :en
      :i18n.language/name "English"}
     {:schema/type :schema.type/i18n.language
      :i18n.language/keyword :es
      :i18n.language/name "Español"}])})



(spec/def :i18n.translation/value string?)

(spec/def :i18n.translation/language
  (spec/with-gen ::entity/ref #(entity/ref-generator :i18n.language)))

(spec/def :schema.type/i18n.translation
  (spec/keys :req [:i18n.translation/value
                   :i18n.translation/language]))

(entity/register-type!
 :i18n.translation
 {:attributes
  [{:db/ident :i18n.translation/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :i18n.translation/language
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:i18n.language}

  :to-json
  (fn [this]
    [(:i18n.language/keyword (entity/find (:i18n.translation/language this)))
     (:i18n.translation/value this)])})



(spec/def :i18n/translations
  (spec/with-gen ::entity/refs #(entity/refs-generator :i18n.translation)))

(spec/def :schema.type/i18n
  (spec/keys :req [:i18n/translations]))

(entity/register-type!
 :i18n
 {:attributes
  [{:db/ident :i18n/translations
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}]

  :dependencies
  #{:i18n.translation}

  :to-json
  (fn [this]
    (into {}
          (map (comp entity/to-json entity/find) (:i18n/translations this))))})



(spec/def ::ref
  (spec/with-gen ::entity/ref #(entity/ref-generator :i18n)))

(defn get-i18n-entity [translations]
  {:schema/type :schema.type/i18n
   :i18n/translations (map (fn [[language-kw value]]
                             {:schema/type :schema.type/i18n.translation
                              :i18n.translation/value value
                              :i18n.translation/language [:i18n.language/keyword language-kw]})
                           translations)})
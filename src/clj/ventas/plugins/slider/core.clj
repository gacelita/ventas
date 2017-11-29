(ns ventas.plugins.slider.core
  (:require
   [ventas.plugin :as plugin]
   [ventas.server.api :as api]
   [ventas.database.entity :as entity]
   [ventas.entities.i18n :as entities.i18n]
   [ventas.entities.file :as entities.file]
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]))

(plugin/register!
 :slider
 {:version "0.1"
  :name "Slider"})

(spec/def :slider.slide/name ::entities.i18n/ref)

(spec/def :slider.slide/file ::entities.file/ref)

(spec/def :schema.type/slider.slide
  (spec/keys :req [:slider.slide/name
                   :slider.slide/file]))

(entity/register-type!
 :slider.slide
 {:attributes
  [{:db/ident :slider.slide/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}
   {:db/ident :slider.slide/file
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]

  :dependencies
  #{:i18n :file}

  :autoresolve? true})

(spec/def :slider.slider/name ::entities.i18n/ref)

(spec/def :slider.slider/keyword keyword?)

(spec/def :slider.slider/slides
  (spec/with-gen ::entity/refs #(entity/refs-generator :slider.slide)))

(spec/def :slider.slider/auto boolean?)

(spec/def :slider.slider/auto-speed
  (spec/with-gen integer?
                 #(gen/choose 2000 6000)))

(spec/def :schema.type/slider.slider
  (spec/keys :req [:slider.slider/name
                   :slider.slider/slides
                   :slider.slider/keyword
                   :slider.slider/auto
                   :slider.slider/auto-speed]))

(entity/register-type!
 :slider.slider
 {:attributes
  [{:db/ident :slider.slider/name
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true}
   {:db/ident :slider.slider/slides
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :slider.slider/keyword
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :slider.slider/auto
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether this slider should move to the next slide automatically"}
   {:db/ident :slider.slider/auto-speed
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The speed for the :auto option, in ms"}]

  :dependencies
  #{:slider.slide :i18n}

  :fixtures
  (fn []
    [{:schema/type :schema.type/slider.slider
      :slider.slider/name (entities.i18n/get-i18n-entity {:en_US "Sample slider"})
      :slider.slider/keyword :sample-slider
      :slider.slider/auto true
      :slider.slider/auto-speed 4000
      :slider.slider/slides [{:schema/type :schema.type/slider.slide
                              :slider.slide/name (entities.i18n/get-i18n-entity {:en_US "First slide"})
                              :slider.slide/file (first (entity/query :file))}
                             {:schema/type :schema.type/slider.slide
                              :slider.slide/name (entities.i18n/get-i18n-entity {:en_US "Second slide"})
                              :slider.slide/file (second (entity/query :file))}]}])})

(api/register-endpoint!
  ::sliders.get
  (fn [{:keys [params] :as message} state]
    (let [{:keys [keyword]} params]
      (if-let [slider (entity/find [:slider.slider/keyword keyword])]
        (entity/to-json slider)
        (throw (Error. (str "Could not find slider with keyword: " keyword)))))))

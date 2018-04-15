(ns ventas.plugin
  (:refer-clojure :exclude [find])
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.core.async :refer [go-loop <!]]
   [ventas.database.schema :as schema]
   [ventas.utils :as utils]
   [ventas.events :as events]
   [ventas.entities.i18n :as entities.i18n]
   [slingshot.slingshot :refer [throw+]]))

(spec/def ::name
  (spec/or :string string?
           :i18n ::entities.i18n/ref))

(spec/def ::attrs
  (spec/keys :req-un [::name]))

(defonce plugins (atom {}))

(defn register! [kw {:keys [migrations] :as attrs}]
  {:pre [(keyword? kw) (utils/check ::attrs attrs)]}
  (swap! plugins assoc kw attrs)
  (doseq [migration migrations]
    (schema/register-migration! migration)))

(defn find [kw]
  {:pre [(keyword? kw)]}
  (get @plugins kw))

(defn all []
  @plugins)

(go-loop []
  (<! (events/sub :init))
  (doseq [[_ {:keys [init]}] (all)]
    (when init
      (init)))
  (recur))

(defn by-type
  "Returns the plugins with the given type"
  [type]
  (->> (all)
       (filter (fn [[_ v]]
                 (if (= type :plugin)
                   (not (:type v))
                   (= (:type v) type))))
       (into {})))

(defn check! [kw]
  {:pre [(keyword? kw)]}
  (if-not (find kw)
    (throw+ {:type ::plugin-not-found
             :keyword kw})
    true))

(defn fixtures [kw]
  {:pre [(keyword? kw)]}
  (check! kw)
  (when-let [fixtures-fn (:fixtures (find kw))]
    (fixtures-fn)))

(defn handle-request
  "Processes HTTP requests directed to the plugins"
  [kw path]
  {:pre [(check! kw)]}
  (let [plugin (find kw)]
    ((:http-handler plugin) path)))

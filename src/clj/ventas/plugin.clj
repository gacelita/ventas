(ns ventas.plugin
  (:refer-clojure :exclude [find])
  (:require
   [clojure.core.async :refer [<! go-loop]]
   [clojure.spec.alpha :as spec]
   [slingshot.slingshot :refer [throw+]]
   [ventas.database.schema :as schema]
   [hiccup.compiler :refer [compile-html]]
   [ventas.events :as events]
   [ventas.utils :as utils]
   [hiccup.core :as hiccup]
   [clojure.string :as str]))

(spec/def ::plugin
  (spec/keys :req-un [::type]
             :opt-un [::fixtures
                      ::migrations]))

(defmulti plugin-types :type)

(defmethod plugin-types :theme [_]
  (spec/and
   ::plugin
   (spec/keys :req-un [::build]
              :opt-un [::init-script
                       ::default?
                       ::should-load?])))

(defmethod plugin-types :default [_]
  ::plugin)

(spec/def ::attrs
  (spec/multi-spec plugin-types :type))

(defonce plugins (atom {}))

(defn register! [kw {:keys [migrations] :as attrs}]
  {:pre [(keyword? kw) (utils/check ::attrs attrs)]}
  (swap! plugins assoc kw attrs)
  (doseq [[key attributes] migrations]
    (schema/register-migration! (keyword (name kw) (name key)) attributes)))

(defn find [kw]
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
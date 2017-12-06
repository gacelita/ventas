(ns ventas.plugin
  (:require
   [ventas.utils :as utils]
   [clojure.spec.alpha :as spec]
   [clojure.core.async :refer [<! go go-loop]]
   [ventas.database :as db]
   [io.rkn.conformity :as conformity]
   [ventas.database.schema :as schema]))

(spec/def ::version string?)

(spec/def ::name string?)

(spec/def ::plugin-attrs
  (spec/keys :req-un [::version
                      ::name]))

(defonce plugins (atom {}))

(defn register! [kw attrs]
  {:pre [(keyword? kw) (utils/check ::plugin-attrs attrs)]}
  (swap! plugins assoc kw attrs))

(defn plugin [kw]
  {:pre [(keyword? kw)]}
  (get @plugins kw))

(defn all []
  (set (keys @plugins)))

(defn check! [kw]
  {:pre [(keyword? kw)]}
  (if-not (plugin kw)
    (throw (Exception. (str "The plugin " kw " is not registered")))
    true))

(defn fixtures [plugin-kw]
  (when-let [fixtures-fn (:fixtures (plugin plugin-kw))]
    (fixtures-fn)))

(defn register-migration!
  "Registers database attributes for this plugin.
   Do not use this for registering entities: use entity/register-type! instead,
   and specify the entities' attributes there."
  [plugin-kw attrs]
  {:pre [(check! plugin-kw) (coll? attrs)]}
  (let [{plugin-version :version} (plugin plugin-kw)
        attrs (map (fn [attr]
                     (-> attr
                         (assoc :ventas/pluginId plugin-kw)
                         (assoc :ventas/pluginVersion plugin-version)))
                   attrs)]
    (schema/register-migration! attrs)))


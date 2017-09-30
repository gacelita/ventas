(ns ventas.plugin
  (:require
   [ventas.util :as util]
   [clojure.spec.alpha :as spec]
   [clojure.core.async :refer [<! go go-loop]]
   [ventas.database :as db]
   [io.rkn.conformity :as conformity]
   [ventas.events :as events]
   [ventas.database.schema :as schema]))

(spec/def ::version string?)
(spec/def ::name string?)
(spec/def ::plugin-attrs
  (spec/keys :req-un [::version ::name]))

(defonce plugins (atom {}))

(defn register! [kw attrs]
  {:pre [(keyword? kw) (util/check ::plugin-attrs attrs)]}
  (swap! plugins assoc kw attrs))

(defn plugin [kw]
  {:pre [(keyword? kw)]}
  (get @plugins kw))

(defn check-plugin [kw]
  {:pre [(keyword? kw)]}
  (if-not (plugin kw)
    (throw (Exception. (str "The plugin " kw " is not registered")))
    true))

(defn register-plugin-migration!
  "Registers database attributes for this plugin.
   Do not use this for registering entities: use entity/register-type! instead,
   and specify the entities' attributes there."
  [plugin-kw attrs]
  {:pre [(check-plugin plugin-kw) (coll? attrs)]}
  (let [{plugin-version :version} (plugin plugin-kw)
        attrs (map (fn [attr]
                     (-> attr
                         (assoc :ventas/pluginId plugin-kw)
                         (assoc :ventas/pluginVersion plugin-version)))
                   attrs)]
    (clojure.pprint/pprint attrs)
    (schema/register-migration! attrs)))
(ns ventas.plugin
  (:refer-clojure :exclude [filter])
  (:require [ventas.util :as util]
            [clojure.spec.alpha :as spec]
            [ventas.database :as db]
            [io.rkn.conformity :as conformity]))

(spec/def ::version string?)
(spec/def ::name string?)
(spec/def ::plugin-attrs
  (spec/keys :req-un [::version ::name]))

(defonce plugins (atom {}))

(defn register! [kw attrs]
  {:pre [(keyword? kw) (util/check ::plugin-attrs attrs)]}
  (swap! plugins kw attrs))

(defn plugin [kw]
  {:pre [(keyword? kw)]}
  (get @plugins kw))

(defn db-attributes! [plugin-kw attrs]
  {:pre [(keyword? plugin-kw) (vector? attrs)]}
  (when-not (plugin plugin-kw)
    (throw (Exception. "The plugin " plugin-kw " is not registered")))
  (let [{plugin-version :version} (plugin plugin-kw)
        attrs (map (fn [attr]
                     (-> attr
                         (assoc :ventas/pluginId plugin-kw)
                         (assoc :ventas/pluginVersion plugin-version)))
                   attrs)])
  (conformity/ensure-conforms
   db/db
   {(hash attrs)
    {:txes [attrs]}}))
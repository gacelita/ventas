(ns ventas.plugin
  (:require
   [ventas.util :as util]
   [clojure.spec.alpha :as spec]
   [clojure.core.async :refer [<! go]]
   [ventas.database :as db]
   [io.rkn.conformity :as conformity]
   [ventas.events :as events]))

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

(defn db-attributes! [plugin-kw attrs]
  {:pre [(keyword? plugin-kw) (vector? attrs)]}
  (go
    (when (<! events/init)
      (when-not (plugin plugin-kw)
        (throw (Exception. (str "The plugin " plugin-kw " is not registered"))))
      (let [{plugin-version :version} (plugin plugin-kw)
            attrs (map (fn [attr]
                         (-> attr
                             (assoc :ventas/pluginId plugin-kw)
                             (assoc :ventas/pluginVersion plugin-version)))
                       attrs)]
        (clojure.pprint/pprint attrs)
        (conformity/ensure-conforms
         db/db
         {(keyword (str "plugin-migration-" (hash attrs)))
          {:txes [attrs]}})))))
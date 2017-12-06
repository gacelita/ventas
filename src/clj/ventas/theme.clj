(ns ventas.theme
  (:require
   [ventas.utils :as utils]
   [clojure.spec.alpha :as spec]
   [ventas.database.schema :as schema]))

(spec/def ::version string?)

(spec/def ::name string?)

(spec/def ::attrs
  (spec/keys :req-un [::version
                      ::name]))

(defonce themes (atom {}))

(defn register! [kw attrs]
  {:pre [(keyword? kw) (utils/check ::attrs attrs)]}
  (swap! themes assoc kw attrs))

(defn theme [kw]
  {:pre [(keyword? kw)]}
  (get @themes kw))

(defn all []
  (set (keys @themes)))

(defn check! [kw]
  {:pre [(keyword? kw)]}
  (if-not (theme kw)
    (throw (Exception. (str "The theme " kw " is not registered")))
    true))

(defn fixtures [kw]
  {:pre [(keyword? kw)]}
  (check! kw)
  (when-let [fixtures-fn (:fixtures (theme kw))]
    (fixtures-fn)))

(defn register-migration!
  "Registers database attributes for this theme."
  [theme-kw attrs]
  {:pre [(check! theme-kw) (coll? attrs)]}
  (schema/register-migration! attrs))


(ns ventas.plugin
  (:require
   [clojure.spec.alpha :as spec]
   [ventas.database.schema :as schema]
   [ventas.utils :as utils]))

(spec/def ::name string?)

(spec/def ::attrs
  (spec/keys :req-un [::name]))

(defonce plugins (atom {}))

(defn register! [kw {:keys [migrations] :as attrs}]
  {:pre [(keyword? kw) (utils/check ::attrs attrs)]}
  (swap! plugins assoc kw attrs)
  (doseq [migration migrations]
    (schema/register-migration! migration)))

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

(defn fixtures [kw]
  {:pre [(keyword? kw)]}
  (check! kw)
  (when-let [fixtures-fn (:fixtures (plugin kw))]
    (fixtures-fn)))

(defn handle-request
  "Processes HTTP requests directed to the plugins"
  [plugin-kw path]
  {:pre [(check! plugin-kw)]}
  (let [plugin (plugin plugin-kw)]
    ((:http-handler plugin) path)))

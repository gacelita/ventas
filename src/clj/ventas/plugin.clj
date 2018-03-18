(ns ventas.plugin
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.core.async :refer [go-loop <!]]
   [ventas.database.schema :as schema]
   [ventas.utils :as utils]
   [ventas.events :as events]))

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

(go-loop []
  (<! (events/sub :init))
  (doseq [[_ {:keys [init]}] @plugins]
    (when init
      (init)))
  (recur))

(defn by-type
  "Returns the identifiers of the plugins with the given type"
  [type]
  (->> (all)
       (map (fn [k]
              [k (plugin k)]))
       (filter (fn [[k v]]
                 (if (= type :plugin)
                   (not (:type v))
                   (= (:type v) type))))
       (into {})
       (keys)))

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

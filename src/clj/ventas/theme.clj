(ns ventas.theme
  (:require
   [ventas.utils :as utils]
   [clojure.spec.alpha :as spec]
   [ventas.database.schema :as schema]
   [ventas.entities.configuration :as entities.configuration]))

(spec/def ::name string?)

(spec/def ::attrs
  (spec/keys :req-un [::name]))

(defonce themes (atom {}))

(defn register! [kw {:keys [migrations] :as attrs}]
  {:pre [(keyword? kw) (utils/check ::attrs attrs)]}
  (swap! themes assoc kw attrs)
  (doseq [migration migrations]
    (schema/register-migration! migration)))

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

(defn current []
  (theme
   (or (entities.configuration/get :current-theme)
       (first (all)))))

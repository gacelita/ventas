(ns ventas.theme
  (:require [ventas.utils :as utils]
            [clojure.spec.alpha :as spec]))

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
(ns ventas.widget
  (:refer-clojure :exclude [find]))

(defonce ^:private registry (atom {}))

(defonce ^:private pages (atom nil))

(defn register! [kw attrs]
  {:pre [(keyword? kw)]}
  (swap! registry assoc kw attrs))

(defn set-pages! [v]
  (reset! pages v))

(defn find [kw]
  (get @registry kw))

(defn get-pages []
  @pages)
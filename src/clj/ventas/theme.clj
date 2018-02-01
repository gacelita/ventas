(ns ventas.theme
  (:require
   [ventas.plugin :as plugin]
   [ventas.entities.configuration :as entities.configuration]))

(defn register! [kw attrs]
  (plugin/register! kw (merge attrs
                              {:type :theme})))

(defn all []
  (->> (plugin/all)
       (map (fn [k]
              [k (plugin/plugin k)]))
       (filter (fn [[k v]]
                 (= (:type v) :theme)))
       (into {})
       (keys)))

(defn current []
  (plugin/plugin
   (or (entities.configuration/get :current-theme)
       (first (all)))))

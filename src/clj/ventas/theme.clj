(ns ventas.theme
  (:require
   [ventas.database.entity :as entity]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.plugin :as plugin]))

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
  (or (keyword (entities.configuration/get :current-theme))
      (first (all))))

(defn set!
  "Sets the current theme.
   Use repl/set-theme! in development."
  [theme]
  (entity/create :configuration {:keyword :current-theme
                                 :value (name theme)}))

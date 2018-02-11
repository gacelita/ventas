(ns ventas.theme
  (:require
   [ventas.database.entity :as entity]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.plugin :as plugin]))

(defn register! [kw attrs]
  (plugin/register! kw (merge attrs
                              {:type :theme})))

(defn all []
  (plugin/by-type :theme))

(defn current []
  (or (keyword (entities.configuration/get :current-theme))
      (first (all))))

(defn set!
  "Sets the current theme.
   Use repl/set-theme! in development."
  [theme]
  (entity/create :configuration {:keyword :current-theme
                                 :value (name theme)}))

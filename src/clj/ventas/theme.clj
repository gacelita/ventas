(ns ventas.theme
  (:require
   [ventas.entities.configuration :as entities.configuration]
   [ventas.plugin :as plugin]
   [ventas.config :as config]))

(defn register! [kw attrs]
  (plugin/register! kw (merge attrs
                              {:type :theme})))

(defn all []
  (plugin/by-type :theme))

(defn current []
  (or (config/get :theme)
      (entities.configuration/get :theme)
      (some-> (all) (first) (key))))
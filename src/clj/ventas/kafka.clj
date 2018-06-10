(ns ventas.kafka
  (:require
   [ventas.config :as config]))

(defn enabled? []
  (boolean (config/get :kafka :host)))

(defn url []
  (str (config/get :kafka :host)
       ":"
       (config/get :kafka :port)))
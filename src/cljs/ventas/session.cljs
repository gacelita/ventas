(ns ventas.session
  (:require
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.events :as events]))

(defn get-identity []
  (let [session @(rf/subscribe [::events/db [:session]])]
    (when (not (get-in session [:identity :id]))
      (routes/go-to :frontend.login))
    (:identity session)))

(ns ventas.plugins.featured-categories.api
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::featured-categories.list
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :ventas.plugins.featured-categories.core/featured-categories.list}
                       options)}))

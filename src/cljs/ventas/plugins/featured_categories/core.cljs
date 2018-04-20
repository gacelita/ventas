(ns ventas.plugins.featured-categories.core
  (:require
   [re-frame.core :as rf]
   [ventas.components.category-list :as components.category-list]
   [ventas.events :as events]
   [ventas.plugins.featured-categories.api :as backend]))

(rf/reg-event-fx
 ::featured-categories.list
 (fn [_ _]
   {:dispatch [::backend/featured-categories.list
               {:success [::events/db [::featured-categories]]}]}))

(defn featured-categories []
  (let [categories @(rf/subscribe [::events/db [::featured-categories]])]
    [components.category-list/category-list categories]))

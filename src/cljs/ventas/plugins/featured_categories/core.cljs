(ns ventas.plugins.featured-categories.core
  (:require
   [ventas.plugins.featured-categories.api :as backend]
   [ventas.components.category-list :as components.category-list]
   [re-frame.core :as rf]
   [ventas.events :as events]))

(rf/reg-event-fx
 ::featured-categories.list
 (fn [cofx [_]]
   {:dispatch [::backend/featured-categories.list
               {:success #(rf/dispatch [::events/db [::featured-categories] %])}]}))

(defn featured-categories []
  (rf/dispatch [::featured-categories.list])
  (fn []
    (let [categories @(rf/subscribe [::events/db [::featured-categories]])]
      [components.category-list/category-list categories])))

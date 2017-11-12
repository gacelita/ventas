(ns ventas.plugins.featured-categories.core
  (:require
   [ventas.plugins.featured-categories.api :as api]
   [ventas.components.category-list :as components.category-list]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::featured-categories.list
 (fn [cofx [_]]
   {:dispatch [::api/featured-categories.list
               {:success-fn #(rf/dispatch [:ventas/db [::featured-categories] %])}]}))

(defn featured-categories []
  (rf/dispatch [::featured-categories.list])
  (fn []
    (let [categories @(rf/subscribe [:ventas/db [::featured-categories]])]
      [components.category-list/category-list categories])))

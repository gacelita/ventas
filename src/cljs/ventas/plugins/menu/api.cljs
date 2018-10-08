(ns ventas.plugins.menu.api
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::autocompletions.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :ventas.plugins.menu.core/autocompletions.get}
                       options)}))

(rf/reg-event-fx
 ::routes.get-name
 (fn [_ [_ options]]
   {:ws-request (merge {:name :ventas.plugins.menu.core/routes.get-name}
                       options)}))

(rf/reg-event-fx
 ::menus.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :ventas.plugins.menu.core/menu.get}
                       options)}))
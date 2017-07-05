(ns ventas.components.notificator
  (:require [ventas.util :as util]
            [soda-ash.core :as sa]
            [clojure.string :as s]
            [fqcss.core :refer [wrap-reagent]]
            [cljs.core.async :refer [<! >! put! close! timeout chan]]
            [re-frame.core :as rf])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(rf/reg-sub :app/notifications
  (fn [db _] (-> db :notifications)))

(rf/reg-event-db :app/notifications.add
 (fn event-notifications-add [db [_ notification]]
   (let [sym (gensym)
         notification (assoc notification :sym sym)]
     (go
      (<! (timeout 4000))
      (rf/dispatch [:app/notifications.remove sym]))
     (if (vector? (:notifications db))
       (assoc db :notifications (conj (:notifications db) notification))
       (assoc db :notifications [notification])))))

(rf/reg-event-db :app/notifications.remove
  (fn event-notifications-remove [db [_ sym]]
    (assoc db :notifications (remove #(= (:sym %) sym) (:notifications db)))))

(defn notificator []
  "Displays notifications"
  (wrap-reagent
    [:div {:fqcss [::notificator]}
      (for [notification @(rf/subscribe [:app/notifications])]
        [:div {:fqcss [::notification] :class (:theme notification)}
          [sa/Icon {:class "bu close" :name (:icon notification) :on-click #(rf/dispatch [:app/notifications.remove (:sym notification)])}]
          [:p {:class "bu message"} (:message notification)]])]))
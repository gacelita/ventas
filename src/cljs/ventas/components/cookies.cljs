(ns ventas.components.cookies
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]))

(def state-key ::state)

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (assoc db state-key :closed)))

(rf/reg-event-db
 ::open
 (fn [db [_]]
   (assoc db state-key :opened)))

(defn cookies
  "Cookie warning"
  [text]
  (let [state @(rf/subscribe [::events/db [state-key]])]
    [:div.cookies {:style (when (= state :closed) {:max-height "0px"})}
     [:p text]
     [base/icon {:name "remove"
                 :on-click #(rf/dispatch [::close])}]]))
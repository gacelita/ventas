(ns ventas.components.cookies
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(def state-key ::state)

(rf/reg-event-fx
 ::get-state-from-local-storage
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [db local-storage]} [_]]
   {:db (assoc db state-key (get local-storage state-key))}))

(rf/reg-sub
 ::open?
 (fn [db]
   (not (get db state-key))))

(rf/reg-event-fx
 ::close
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [db local-storage]}]
   {:db (assoc db state-key true)
    :local-storage (assoc local-storage state-key true)}))

(defn cookies [text]
  "Cookie warning"
  (let [open? @(rf/subscribe [::open?])]
    [:div.cookies {:style (when-not open? {:max-height "0px"})}
     [:p text]
     [base/icon {:name "remove"
                 :on-click #(rf/dispatch [::close])}]]))

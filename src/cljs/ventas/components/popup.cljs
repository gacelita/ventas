(ns ventas.components.popup
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]))

(def data-key ::popup)

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (-> db (update data-key drop-last))))

(rf/reg-event-db
 ::show
  (fn [db [_ title message]]
    (let [data {:open true :message message :title title}]
      (if (seq (get db data-key))
        (update db data-key conj data)
        (assoc db data-key [data])))))

(defn popup
  "A popup, useful for displaying messages to the user"
  []
  [:div.popup
   (let [items @(rf/subscribe [::events/db [data-key]])]
     (when-let [data (last items)]
       [base/modal {:basic true :open (:open data) :size "small"}
        [base/header
         [base/icon {:name "remove"}]
         [:div.content {:title data}]
         [:div.popup__counter
          (str (count items) "/" (count items))]]
        [base/modalContent
         [:p (:message data)]]
        [base/modalActions
         [base/button {:color "green"
                       :inverted true
                       :on-click #(rf/dispatch [::close])}
          [base/icon {:name "checkmark"}]
          "OK"]]]))])


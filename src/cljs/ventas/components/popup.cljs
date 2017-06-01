(ns ventas.components.popup
  (:require [ventas.util :as util]
            [soda-ash.core :as sa]
            [clojure.string :as s]
            [reagent.core :as reagent]
            [fqcss.core :refer [wrap-reagent]]
            [re-frame.core :as rf]))

(rf/reg-sub :components/popup
  (fn [db _] (-> db :components :popup)))

(rf/reg-event-db :components.popup/close
  (fn [db [_]]
    (-> db (update-in [:components :popup] drop-last))))

(rf/reg-event-db :components.popup/show
  (fn [db [_ title message]]
    (js/console.log "Title" title "message" message)
    (let [data {:open true :message message :title title}]
      (if (vector? (get-in db [:components :popup]))
        (update-in db [:components :popup] conj data)
        (assoc-in db [:components :popup] [data])))))

(defn popup
  "A popup, useful for displaying messages to the user"
  []
  (wrap-reagent
   [:div.ventas {:fqcss [::popup]}
    (let [items @(rf/subscribe [:components/popup])]
      (if-let [data (last items)]
        [sa/Modal {:basic true :open (:open data) :size "small"}
         [sa/Header
          [sa/Icon {:name "remove"}]
          [:div.content (:title data)]
          [:div.ventas {:fqcss [::popup-counter]} (str (count items) "/" (count items))]]
         [sa/ModalContent
          [:p (do (js/console.log data) (:message data))]]
         [sa/ModalActions
          [sa/Button {:color "green" :inverted true :on-click #(rf/dispatch [:components.popup/close])}
           [sa/Icon {:name "checkmark"}] "OK"]]]))]))


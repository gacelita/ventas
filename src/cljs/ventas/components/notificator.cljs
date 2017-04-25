(ns ventas.components.notificator
  (:require [ventas.util :as util]
            [soda-ash.core :as sa]
            [re-frame.core :as rf]))

(defn bu-notificator []
  "Displays notifications"
  [:div {:class "bu notificator"}
    (for [notification @(rf/subscribe [:app/notifications])]
      [:div {:class (s/join " " ["bu" "notification" (:theme notification)])}
        [sa/Icon {:class "bu close" :name (:icon notification) :on-click #(rf/dispatch [:app/notifications.remove (:sym notification)])}]
        [:p {:class "bu message"} (:message notification)]
      ])])
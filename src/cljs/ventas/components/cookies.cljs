(ns ventas.components.cookies
  (:require [fqcss.core :refer [wrap-reagent]]
            [soda-ash.core :as sa]
            [re-frame.core :as rf]))

(rf/reg-sub ::state
            (fn [db _] (-> db ::state)))

(rf/reg-event-db ::close
                 (fn [db [_]]
                   (assoc db ::state :closed)))

(rf/reg-event-db ::open
                 (fn [db [_]]
                   (assoc db ::state :opened)))

(defn cookies
  "Cookie warning"
  [text]
  (wrap-reagent
   [:div {:fqcss [::cookies] :style (when (= @(rf/subscribe [::state]) :closed) {:max-height "0px"})}
    [:p text]
    [sa/Icon {:name "remove" :on-click #(rf/dispatch [::close])}]]))
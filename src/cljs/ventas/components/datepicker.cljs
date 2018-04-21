(ns ventas.components.datepicker
  (:require
   [cljsjs.react-date-range]
   [re-frame.core :as rf]
   [reagent.ratom :refer [atom]]
   [ventas.components.base :as base]
   [ventas.events :as events]))

(def state-key ::state)

(rf/reg-event-fx
 ::set-value
 (fn [_ [_ id on-change-fx {:strs [startDate endDate]}]]
   {:dispatch-n
    [[::events/db [state-key id] (str (.format startDate "YYYY-MM-DD")
                                      " / "
                                      (.format endDate "YYYY-MM-DD"))]
     (conj on-change-fx {:start startDate
                         :end endDate})]}))

(defn range-picker [{:keys [on-change-fx]}]
  (js/React.createElement
   js/ReactDateRange.DateRange
   #js {:onChange #(rf/dispatch (conj on-change-fx (js->clj %)))}))

(defn range-input [_]
  (let [focused? (atom false)
        id (str (gensym))]
    (fn [{:keys [placeholder on-change-fx]}]
      [:div.date-range-input
       [base/input
        [:input {:on-focus #(reset! focused? true)
                 :on-blur #(reset! focused? false)
                 :readOnly true
                 :value (or @(rf/subscribe [::events/db [state-key id]])
                            "")
                 :placeholder placeholder}]]
       (when @focused?
         [range-picker {:on-change-fx [::set-value id on-change-fx]}])])))

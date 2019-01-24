(ns ventas.components.datepicker
  (:require
   [react-date-range]
   [re-frame.core :as rf]
   [reagent.ratom :refer [atom]]
   [ventas.components.base :as base]
   [ventas.utils :as utils]
   [reagent.core :as reagent]))

(def state-key ::state)

(rf/reg-event-fx
 ::set-value
 (fn [_ [_ id on-change-fx {:strs [startDate endDate]}]]
   {:dispatch-n
    [[:db [state-key id] (str (.format startDate "YYYY-MM-DD")
                                      " / "
                                      (.format endDate "YYYY-MM-DD"))]
     (conj on-change-fx {:start startDate
                         :end endDate})]}))

(defn range-picker [{:keys [on-change-fx]}]
  (js/React.createElement
   js/ReactDateRange.DateRange
   #js {:onChange #(rf/dispatch (conj on-change-fx (js->clj %)))}))

(defn- handle-input-click [this-node focused? target]
  (when-not (utils/child? target this-node)
    (reset! focused? false)))

(defn range-input [_]
  (let [focused? (atom false)
        id (str (gensym))
        node (atom nil)
        click-listener #(handle-input-click @node focused? (.-target %))]
    (reagent/create-class
     {:component-will-unmount
      (fn [_]
        (.removeEventListener js/window "click" click-listener))
      :component-did-mount
      (fn [this]
        (reset! node (reagent/dom-node this))
        (.addEventListener js/window "click" click-listener))
      :reagent-render
      (fn [{:keys [placeholder on-change-fx]}]
        [:div.date-range-input
         [base/input
          [:input {:on-focus #(reset! focused? true)
                   :readOnly true
                   :value (or @(rf/subscribe [:db [state-key id]])
                              "")
                   :placeholder placeholder}]]
         (when @focused?
           [range-picker {:on-change-fx [::set-value id on-change-fx]}])])})))


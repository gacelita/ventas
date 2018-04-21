(ns ventas.components.amount-input
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [ventas.components.base :as base]
   [ventas.events :as events]))

(rf/reg-event-fx
 ::set-field
 (fn [_ [_ amount k v on-change-fx]]
   {:dispatch (conj on-change-fx
                    (assoc amount k v
                                  :schema/type :schema.type/amount))}))

(rf/reg-event-fx
 ::set-currency
 (fn [_ [_ amount v on-change-fx]]
   {:dispatch [::set-field amount :amount/currency v on-change-fx]}))

(rf/reg-event-fx
 ::set-value
 (fn [_ [_ amount v on-change-fx]]
   {:dispatch [::set-field amount :amount/value v on-change-fx]}))

(defn- dropdown [{:keys [amount on-change-fx]}]
  [:div.amount-input__currency
   [base/form-field
    [base/dropdown
     {:fluid true
      :selection true
      :options (map (fn [v]
                      {:text (:symbol v)
                       :value (:id v)})
                    @(rf/subscribe [::events/db [:admin :currencies]]))
      :default-value (get-in amount [:amount/currency :db/id])
      :on-change #(rf/dispatch [::set-currency amount (js/parseFloat (.-value %2)) on-change-fx])}]]])

(defn input [{:keys [amount control label on-change-fx]}]
  [base/form-input {:label label :key (boolean amount)}
   [dropdown {:amount amount
              :on-change-fx on-change-fx}]
   [(or control :input)
    {:default-value (common.utils/bigdec->str (get amount :amount/value))
     :on-change #(rf/dispatch [::set-value amount (common.utils/str->bigdec (-> % .-target .-value)) on-change-fx])}]])

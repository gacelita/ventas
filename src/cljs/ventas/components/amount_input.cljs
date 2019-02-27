(ns ventas.components.amount-input
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [ventas.components.base :as base]))

(rf/reg-event-fx
 ::set-field
 (fn [_ [_ amount ks v on-change-fx]]
   {:dispatch (conj on-change-fx
                    (-> amount
                        (assoc :schema/type :schema.type/amount)
                        (assoc-in ks v)))}))

(rf/reg-event-fx
 ::set-currency
 (fn [_ [_ amount v on-change-fx]]
   {:dispatch [::set-field amount [:amount/currency :db/id] v on-change-fx]}))

(rf/reg-event-fx
 ::set-value
 (fn [_ [_ amount v on-change-fx]]
   {:dispatch [::set-field amount [:amount/value] v on-change-fx]}))

(rf/reg-sub
 ::default-currency
 :<- [:db [:admin :general-config]]
 :<- [:db [:admin :currencies]]
 (fn [[{:general-config/keys [culture]} currencies]]
   (or (->> currencies (filter #(= (-> % :culture :id) culture)) first :id)
       (:id (first currencies)))))

(defn- dropdown [{:keys [amount on-change-fx]}]
  (let [default-currency @(rf/subscribe [::default-currency])]
    ^{:key default-currency}
    [:div.amount-input__currency
     [base/form-field
      [base/dropdown
       {:fluid true
        :selection true
        :options (map (fn [v]
                        {:text (:symbol v)
                         :value (:id v)})
                      @(rf/subscribe [:db [:admin :currencies]]))
        :default-value (or (get-in amount [:amount/currency :db/id])
                           default-currency)
        :on-change #(rf/dispatch [::set-currency amount (js/parseFloat (.-value %2)) on-change-fx])}]]]))

(defn input [{:keys [amount control label on-change-fx]}]
  [base/form-input {:label label :key (boolean amount)}
   [dropdown {:amount amount
              :on-change-fx on-change-fx}]
   [(or control :input)
    {:default-value (common.utils/bigdec->str (get amount :amount/value))
     :on-change #(rf/dispatch [::set-value amount (common.utils/str->bigdec (-> % .-target .-value)) on-change-fx])}]])

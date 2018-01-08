(ns ventas.components.amount-input
  (:require
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [re-frame.core :as rf]))

(def state-key ::state)

(rf/reg-event-fx
 ::set-field
 (fn [{:keys [db]} [_ id k v callback]]
   {:pre [(fn? callback) (number? v) id]}
   (let [new-value (-> (get-in db [state-key id])
                       (assoc k v))]
     (callback (merge {:schema/type :schema.type/amount}
                      new-value))
     {:db (assoc-in db [state-key id] new-value)})))

(rf/reg-event-fx
 ::set-currency
 (fn [{:keys [db]} [_ id v callback]]
   {:dispatch [::set-field id :amount/currency v callback]}))

(rf/reg-event-fx
 ::set-value
 (fn [{:keys [db]} [_ id v callback]]
   {:dispatch [::set-field id :amount/value v callback]}))

(defn- dropdown [{:keys [currency id on-change]}]
  [:div.amount-input__currency
   [base/form-field
    [base/dropdown
     {:fluid true
      :selection true
      :options (map (fn [v]
                      {:text (:symbol v)
                       :value (:id v)})
                    @(rf/subscribe [::events/db [:admin :currencies]]))
      :default-value currency
      :on-change #(rf/dispatch [::set-currency id (js/parseFloat (.-value %2)) on-change])}]]])

(defn input [{:keys [amount control label culture on-change]}]
  (let [id (gensym)]
    (fn []
      (let [{:amount/keys [value currency]} amount]
        [base/form-input {:label label :key (boolean amount)}
         [dropdown {:currency (:db/id currency)
                    :id id
                    :on-change on-change}]
         [(or control :input)
          {:default-value value
           :on-change #(rf/dispatch [::set-value id (js/parseFloat (-> % .-target .-value)) on-change])}]]))))
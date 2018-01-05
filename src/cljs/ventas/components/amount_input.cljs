(ns ventas.components.amount-input
  (:require
   [ventas.components.base :as base]
   [ventas.events :as events]
   [re-frame.core :as rf]))

(def state-key ::state)

(rf/reg-event-fx
 ::set-currency
 (fn [{:keys [db]} [_ id currency-id callback]]
   {:pre [(fn? callback) (number? currency-id) id]}
   (let [currency (->> (get-in db [:admin :currencies])
                       (filter #(= (:id %) currency-id))
                       (first))
         value (get-in db [state-key id])
         new-value (assoc value :currency currency)]
     (callback new-value)
     {:db (assoc-in db [state-key id] new-value)})))

(rf/reg-event-fx
 ::set-value
 (fn [{:keys [db]} [_ id v callback]]
   {:pre [(fn? callback) (number? v) id]}
   (let [new-state (-> (get-in db [state-key id])
                       (assoc :value v))]
     (callback new-state)
     {:db (assoc-in db [state-key id] new-state)})))

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
      :default-value (:id currency)
      :on-change #(rf/dispatch [::set-currency id (js/parseFloat (.-value %2)) on-change])}]]])

(defn input [{:keys [amount control label culture on-change]}]
  (let [id (gensym)]
    (rf/dispatch [::events/db [state-key id] amount])
    (fn []
      (let [{:keys [value currency]} amount]
        [base/form-input {:label label}
         [dropdown {:currency currency
                    :id id
                    :on-change on-change}]
         [(or control :input)
          {:default-value value
           :on-change #(rf/dispatch [::set-value id (js/parseFloat (-> % .-target .-value)) on-change])}]]))))
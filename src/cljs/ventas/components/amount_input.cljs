(ns ventas.components.amount-input
  (:require
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [re-frame.core :as rf]))

(def state-key ::state)

(rf/reg-event-fx
 ::set-currency
 (fn [{:keys [db]} [_ id currency-id callback]]
   {:pre [(fn? callback) (number? currency-id) id]}
   (let [new-value (-> (get-in db [state-key id])
                       (assoc :amount/currency currency-id))]
     (callback new-value)
     {:db (assoc-in db [state-key id] new-value)})))

(rf/reg-event-fx
 ::set-value
 (fn [{:keys [db]} [_ id v callback]]
   {:pre [(fn? callback) (number? v) id]}
   (let [new-state (-> (get-in db [state-key id])
                       (assoc :amount/value v))]
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
      :default-value currency
      :on-change #(rf/dispatch [::set-currency id (js/parseFloat (.-value %2)) on-change])}]]])

(rf/reg-event-fx
 ::init
 (fn [_ [_ id amount]]
   {:dispatch [::backend/admin.entities.find
               {:params {:id amount}
                :success [::init.next id]}]}))

(rf/reg-event-db
 ::init.next
 (fn [db [_ id data]]
   (assoc-in db [state-key id] (-> data
                                   (dissoc :db/id)))))

(defn- input* [{:keys [amount control label culture on-change]}]
  (let [id (gensym)]
    (when amount
      (rf/dispatch [::init id amount]))
    (fn []
      (let [{:amount/keys [value currency] :as state} @(rf/subscribe [::events/db [state-key id]])]
        [base/form-input {:label label :key (boolean state)}
         [dropdown {:currency currency
                    :id id
                    :on-change on-change}]
         [(or control :input)
          {:default-value value
           :on-change #(rf/dispatch [::set-value id (js/parseFloat (-> % .-target .-value)) on-change])}]]))))

(defn input [{:keys [amount] :as args}]
  [input*
   (assoc args :key (boolean amount))])
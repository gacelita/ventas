(ns ventas.components.i18n-input
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]))

(def state-key ::state)

(rf/reg-event-fx
 ::set-focus
 (fn [_ [_ id focus]]
   {:dispatch [::events/db [state-key id :focused?] focus]}))

(rf/reg-event-fx
 ::on-change
 (fn [{:keys [db]} [_ {:keys [culture translation callback id]}]]
   (js/console.log "culture" culture "translation" translation "id" id)
   (let [data (get-in db [state-key id :data])
         new-data (assoc data culture translation)]
     (callback new-data)
     {:db (assoc-in db [state-key id :data] new-data)})))

(defn- culture-view [{:keys [translation culture id label on-change control]}]
  [base/form-input {:label label}
   [:div.i18n-input__culture
    [:span (name culture)]]
   [(or control :input) {:default-value translation
            :on-change #(rf/dispatch [::on-change
                                      {:id id
                                       :culture culture
                                       :translation (-> % .-target .-value)
                                       :callback on-change}])}]])

(defn input [{:keys [label value on-change culture control]}]
  (let [id (str (gensym))]
    (rf/dispatch [::events/db [state-key id :data] value])
    (fn []
      (let [{:keys [focused?]} @(rf/subscribe [::events/db [state-key id]])
            cultures (map :value @(rf/subscribe [::events/db :cultures]))]
        [:div.i18n-input {:class (when focused? "i18n-input--focused")
                          :on-focus #(rf/dispatch [::set-focus id true])
                          :on-blur #(rf/dispatch [::set-focus id false])}
         (let [translation (get value culture)]
           [culture-view {:culture culture
                          :control control
                          :translation translation
                          :id id
                          :on-change on-change
                          :label label}])
         [:div.i18n-input__cultures
          (for [culture (rest cultures)]
            (let [translation (get value culture)]
              [culture-view {:culture culture
                             :control control
                             :translation translation
                             :id id
                             :on-change on-change
                             :key culture}]))]]))))
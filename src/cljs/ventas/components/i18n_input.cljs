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

(defn- to-i18n-entity [data]
  {:schema/type :schema.type/i18n
   :i18n/translations (for [[culture-kw text] data]
                        [{:schema/type :schema.type/i18n.translation
                          :i18n.translation/value text
                          :i18n.translation/culture [:i18n.culture/keyword culture-kw]}])})

(rf/reg-event-fx
 ::on-change
 (fn [{:keys [db]} [_ {:keys [culture translation callback id]}]]
   (let [data (get-in db [state-key id :data])
         new-data (assoc data culture translation)]
     (callback (to-i18n-entity new-data))
     {:db (assoc-in db [state-key id :data] new-data)})))

(defn- culture-view [{:keys [translation culture id label on-change control]}]
  [base/form-input {:label label}
   [:div.i18n-input__culture
    [:span (name culture)]]
   [(or control :input)
    {:default-value translation
     :on-change #(rf/dispatch [::on-change
                               {:id id
                                :culture culture
                                :translation (-> % .-target .-value)
                                :callback on-change}])}]])

(rf/reg-event-fx
 ::init
 (fn [_ [_ id entity]]
   {:dispatch [::backend/admin.entities.find-json
               {:params {:id entity}
                :success [::init.next id]}]}))

(rf/reg-event-db
 ::init.next
 (fn [db [_ id data]]
   (assoc-in db [state-key id :value] (-> data
                                          (dissoc :db/id)))))

(defn- input* [{:keys [label entity on-change culture control]}]
  (let [id (gensym)]
    (when entity
      (rf/dispatch [::init id entity]))
    (fn []
      (let [{:keys [focused? value]} @(rf/subscribe [::events/db [state-key id]])
            cultures (map :value @(rf/subscribe [::events/db :cultures]))]
        [:div.i18n-input {:key (boolean value)
                          :class (when focused? "i18n-input--focused")
                          :on-focus #(rf/dispatch [::set-focus id true])
                          :on-blur #(rf/dispatch [::set-focus id false])}
         [culture-view {:culture culture
                        :control control
                        :translation (get value culture)
                        :id id
                        :on-change on-change
                        :label label}]
         [:div.i18n-input__cultures
          (for [culture (rest cultures)]
            [culture-view {:culture culture
                           :control control
                           :translation (get value culture)
                           :id id
                           :on-change on-change
                           :key culture}])]]))))

(defn input [{:keys [entity] :as args}]
  [input*
   (assoc args :key (boolean entity))])
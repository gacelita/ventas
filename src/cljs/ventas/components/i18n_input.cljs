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
   :i18n/translations (for [[culture text] data]
                        {:schema/type :schema.type/i18n.translation
                         :i18n.translation/value text
                         :i18n.translation/culture {:db/id culture}})})

(defn- on-change-handler [{:keys [translations callback id]}]
  (callback (to-i18n-entity translations)))

(defn- culture-view [{:keys [translations culture id label on-change control]}]
  [base/form-input {:label label}
   [:div.i18n-input__culture
    (let [culture-data (->> @(rf/subscribe [::events/db :cultures])
                              (filter #(= (:value %) culture))
                              first)]
      [:span (when culture-data (name (:keyword culture-data)))])]
   [(or control :input)
    {:default-value (get translations culture)
     :on-change #(on-change (-> translations
                                (assoc culture (-> % .-target .-value))
                                (to-i18n-entity)))}]])

(defn- translation-map [{:i18n/keys [translations]}]
  (->> translations
       (map (fn [{:i18n.translation/keys [value culture]}]
              [(:db/id culture) value]))
       (into {})))

(defn input [_]
  (let [id (gensym)]
    (fn [{:keys [label entity on-change culture control]}]
      (let [{:keys [focused?]} @(rf/subscribe [::events/db [state-key id]])
            cultures (map :value @(rf/subscribe [::events/db :cultures]))
            translations (translation-map entity)]
        [:div.i18n-input {:class (when focused? "i18n-input--focused")
                          :on-focus #(rf/dispatch [::set-focus id true])
                          :on-blur #(rf/dispatch [::set-focus id false])}
         [culture-view {:culture culture
                        :control control
                        :translations translations
                        :id id
                        :on-change on-change
                        :label label}]
         [:div.i18n-input__cultures
          (for [culture (rest cultures)]
            [culture-view {:culture culture
                           :control control
                           :translations translations
                           :id id
                           :on-change on-change
                           :key culture}])]]))))
(ns ventas.themes.clothing.components.address
  (:require
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.utils :as utils :include-macros true]
   [ventas.utils.validation :as validation]))

(def regular-length-validator [::validation/length-error validation/length-validator {:max 30}])

(defn get-form-config [db-path]
  {:db-path db-path
   :validators {::first-name [regular-length-validator]
                ::last-name [regular-length-validator]
                ::company [regular-length-validator]
                ::address [regular-length-validator]
                ::address-second-line [regular-length-validator]
                ::zip [[::validation/length-error validation/length-validator {:max 10}]]
                ::city [regular-length-validator]
                ::state [regular-length-validator]
                ::email [[::validation/email-error validation/email-validator]]
                ::phone [regular-length-validator]
                ::privacy-policy [[::validation/required-error validation/required-validator]]}})

(defn entity->option [entity]
  {:text (:name entity)
   :value (:id entity)})

(rf/reg-event-db
 ::set-options
 (fn [db [_ path data]]
   (assoc-in db path (map entity->option data))))

(rf/reg-event-fx
 ::fetch-states
 (fn [_ [_ db-path country]]
   {:dispatch [::backend/states.list
               {:params {:country country}
                :success [::set-options (conj db-path :states)]}]}))

(rf/reg-event-fx
 ::init
 (fn [_ [_ db-path {:keys [country state] :as address}]]
   {:dispatch-n [[::form/populate
                  (get-form-config db-path)
                  (-> (common.utils/map-keys #(utils/ns-kw %) address)
                      (update ::state :id)
                      (update ::country :id))]
                 (when country
                   [::set-options (conj db-path :countries) [(:id country)]]
                   [::fetch-states (:id country)])
                 (when state
                   [::set-options (conj db-path :states) [(:id state)]])
                 [::backend/countries.list
                  {:success [::set-options (conj db-path :countries)]}]]}))

(defn- field [db-path {:keys [key] :as args}]
  [form/field (merge args
                     {:db-path db-path
                      :label (i18n key)})])

(defn- address [db-path]
  [form/form db-path
   [base/form
    (let [field (partial field db-path)]
      [:div.address-component
       [base/form-group
        [field {:key ::first-name
                :width 8}]

        [field {:key ::last-name
                :width 8}]]

       [base/form-group
        [field {:key ::company
                :width 16}]]

       [base/form-group
        [field {:key ::address
                :width 10}]

        [field {:key ::address-second-line
                :width 6}]]

       [base/form-group
        [field {:key ::city
                :width 16}]]

       [base/form-group
        [field {:key ::country
                :width 6
                :type :combobox
                :on-change-fx [::fetch-states db-path]
                :options @(rf/subscribe [::events/db (conj db-path :countries)])}]

        [field {:key ::state
                :type :combobox
                :width 6
                :options @(rf/subscribe [::events/db (conj db-path :states)])}]

        [field {:key ::zip
                :width 6}]]

       [base/form-group
        [field {:key ::phone
                :width 16}]]])]])

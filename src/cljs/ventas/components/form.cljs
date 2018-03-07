(ns ventas.components.form
  "Form stuff"
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [cljs.reader :as reader]
   [ventas.components.base :as base]
   [ventas.events.backend :as backend]
   [ventas.components.i18n-input :as i18n-input]
   [ventas.components.amount-input :as amount-input]))

(rf/reg-event-db
 ::set-field
 (fn [db [_ db-path field value]]
   {:pre [(vector? db-path)]}
   (let [field (if-not (sequential? field)
                 [field]
                 field)]
     (assoc-in db
               (concat db-path [:form] field)
               value))))

(rf/reg-event-fx
 ::update-field
 (fn [{:keys [db]} [_ db-path field update-fn]]
   {:pre [(vector? db-path)]}
   (let [field (if-not (sequential? field)
                 [field]
                 field)
         new-value (update-fn (get-in db (concat db-path [:form] field)))]
     {:dispatch [::set-field db-path field new-value]})))

(rf/reg-event-db
 ::populate
 (fn [db [_ db-path data]]
   {:pre [(vector? db-path)]}
   (-> db
       (assoc-in (conj db-path :form) data)
       (assoc-in (conj db-path :form-hash) (hash data)))))

(rf/reg-sub
 ::data
 (fn [db [_ db-path]]
   (get-in db (conj db-path :form))))

(defn form [db-path content]
  (let [form @(rf/subscribe [::events/db (conj db-path :form)])
        form-hash @(rf/subscribe [::events/db (conj db-path :form-hash)])]
    (with-meta content {:key form-hash})))

(def ^:private known-keys #{:value :type :db-path :key :label})

(defmulti input (fn [{:keys [type]}] type) :default :default)

(defmethod input :toggle [{:keys [value db-path key] :as args}]
  [base/checkbox
   (merge (apply dissoc args known-keys)
          {:toggle true
           :checked (or value false)
           :on-change #(rf/dispatch [::set-field db-path key (aget %2 "checked")])})])

(defmethod input :radio [{current-value :value :keys [db-path key options] :as args}]
  [:div
   (for [{:keys [value text]} options]
     [base/form-radio
      (merge (apply dissoc args known-keys)
             {:label text
              :value value
              :checked (= value current-value)
              :on-change #(rf/dispatch [::set-field db-path key (aget %2 "value")])})])])

(defmethod input :i18n [{:keys [value db-path key culture]}]
  [i18n-input/input
   {:entity value
    :culture culture
    :on-change #(rf/dispatch [::set-field db-path key %])}])

(defmethod input :i18n-textarea [{:keys [value db-path key culture]}]
  [i18n-input/input
   {:entity value
    :culture culture
    :control :textarea
    :on-change #(rf/dispatch [::set-field db-path key %])}])

(def state-key ::state)

(rf/reg-event-fx
 ::search
 (fn [{:keys [db]} [_ entity-types search]]
   (if (empty? search)
     {:db (update db state-key #(dissoc % :search))}
     {:db (assoc-in db [state-key :search-query] search)
      :dispatch [::backend/search
                 {:params {:search search
                           :entity-types entity-types}
                  :success [::events/db [state-key :search]]}]})))

(defmethod input :entity [{:keys [value db-path key entity-types]}]
  [base/dropdown {:placeholder (i18n ::search)
                  :selection true
                  :default-value (if value (pr-str value) "")
                  :icon "search"
                  :search (fn [options _] options)
                  :options (->> @(rf/subscribe [::events/db [state-key :search]])
                                (map (fn [result]
                                       {:text (:name result)
                                        :value (pr-str (:id result))})))
                  :on-change #(rf/dispatch [::set-field db-path key (reader/read-string (.-value %2))])
                  :on-search-change #(rf/dispatch [::search entity-types (-> % .-target .-value)])}])

(defmethod input :combobox [{:keys [value db-path key options] :as args}]
  [base/dropdown
   (merge (apply dissoc args known-keys)
          {:fluid true
           :selection true
           :default-value (if value (pr-str value) "")
           :on-change #(rf/dispatch [::set-field db-path key (reader/read-string (.-value %2))])
           :options (map (fn [{:keys [text value]}]
                           {:text text
                            :value (pr-str value)})
                         options)})])

(defmethod input :tags [{:keys [value db-path key options forbid-additions]
                         {:keys [in out] :or {in identity out identity}} :xform}]
  [base/dropdown
   {:allowAdditions (not forbid-additions)
    :multiple true
    :fluid true
    :search true
    :selection true
    :options (map (fn [{:keys [text value]}]
                    {:text text
                     :value (pr-str value)})
                  options)
    :default-value (->> value
                        (in)
                        (map pr-str)
                        (set))
    :on-change (fn [_ result]
                 (rf/dispatch [::set-field db-path key
                               (->> (.-value result)
                                    (map reader/read-string)
                                    (out)
                                    (set))]))}])

(defmethod input :amount [{:keys [value db-path key] :as args}]
  [amount-input/input
   {:amount value
    :on-change #(rf/dispatch [::set-field db-path key %])}])

(defn- parse-value [type value]
  (cond
    (= type :number) (js/parseInt value 10)
    :else value))

(defmethod input :default [{:keys [value db-path key type] :as args}]
  [base/form-input
   (merge (apply dissoc args known-keys)
          {:default-value (or value "")
           :type (or type :text)
           :on-change #(rf/dispatch [::set-field db-path key (parse-value type (-> % .-target .-value))])})])

(defn field [{:keys [db-path key label] :as args}]
  [base/form-field
   [:label label]
   [input
    (merge args
           {:value (get-in @(rf/subscribe [::data db-path]) (if (sequential? key)
                                                              key
                                                              [key]))})]])
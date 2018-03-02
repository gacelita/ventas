(ns ventas.components.form
  "Form stuff"
  (:require
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.components.base :as base]))

(rf/reg-event-db
 ::set-field
 (fn [db [_ db-path field value]]
   {:pre [(vector? db-path)]}
   (js/console.log :set-field db-path field value)
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
     {:dispatch [::set-field field new-value]})))

(rf/reg-event-db
 ::populate
 (fn [db [_ db-path data]]
   {:pre [(vector? db-path)]}
   (-> db
       (assoc-in (conj db-path :form) data)
       (assoc-in (conj db-path :form-hash) (hash data)))))

(defn form [db-path content]
  (let [form @(rf/subscribe [::events/db (conj db-path :form)])
        form-hash @(rf/subscribe [::events/db (conj db-path :form-hash)])]
    ^{:key form-hash}
    [content form]))

(def ^:private known-keys #{:value :type :db-path :key :label})

(defmulti input (fn [{:keys [type]}] type) :default :default)

(defmethod input :toggle [{:keys [value db-path key] :as args}]
  [base/checkbox
   (merge (apply dissoc args known-keys)
          {:toggle true
           :checked (or value false)
           :on-change #(rf/dispatch [::set-field db-path key (aget %2 "checked")])})])

(defmethod input :radio [{:keys [value db-path key options] :as args}]
  [:div
   (for [{:keys [id name]} options]
     [base/form-radio
      (merge (apply dissoc args known-keys)
             {:label name
              :value id
              :checked (= id value)
              :on-change #(rf/dispatch [::set-field db-path key (aget %2 "value")])})])])

(defn- parse-value [type value]
  (cond
    (= type :number) (js/parseInt value 10)
    :else value))

(defmethod input :default [{:keys [value db-path key type] :as args}]
  [base/form-input
   (merge (apply dissoc args known-keys)
          {:value (or value "")
           :type (or type :text)
           :on-change #(rf/dispatch [::set-field db-path key (parse-value type (-> % .-target .-value))])})])

(defn field [{:keys [db-path key label] :as args}]
  [base/form-field
   [:label label]
   [input
    (merge args
           {:value @(rf/subscribe [::events/db (concat db-path [:form key])])})]])
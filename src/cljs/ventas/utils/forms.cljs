(ns ventas.utils.forms
  (:require
   [re-frame.core :as rf]
   [ventas.utils :as utils]
   [ventas.utils.validation :as validation]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [clojure.set :as set]))

(defn- set-field! [db {::keys [state-key validators]} field value]
  (let [{:keys [valid? infractions]} (validation/validate validators field value)]
    (assoc-in db
              [state-key ::fields field]
              {:value value
               :valid? valid?
               :infractions infractions})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ config field value]]
   (set-field! db config field value)))

(rf/reg-event-db
 ::populate
 (fn [db [_ {::keys [state-key] :as config} data]]
   (let [last-hash (get-in db [state-key ::population-hash])]
     (if (= last-hash (hash data))
       db
       (let [field-keys (set/union (keys data) (keys (get-in db [state-key ::fields])))]
         (->
          (reduce (fn [acc field-key]
                    (set-field! acc config field-key (get data field-key)))
                  db
                  field-keys)
          (assoc-in [state-key ::population-hash] (hash data))))))))

(defn get-fields [{::keys [state-key]}]
  @(rf/subscribe [:ventas/db [state-key ::fields]]))

(defn get-field [{::keys [state-key]} field]
  @(rf/subscribe [:ventas/db [state-key ::fields field]]))

(defn get-values [config]
  (->> (get-fields config)
       (map (fn [[k v]]
              [k (:value v)]))
       (into {})))

(defn get-value [config field]
  (:value (get-field config field)))

(defn- report-infraction [{:keys [identifier field value params]}]
  (apply i18n identifier (vals params)))

(defn text-input [{::keys [state-key] :as config} field]
  (let [{:keys [valid? value infractions]} (get-field config field)]
    [:div
     [base/form-input {:error (and (not (nil? valid?)) (not valid?))
                       :label (i18n field)
                       :default-value value
                       :on-change (utils/value-handler
                                   #(rf/dispatch [::set-field config field %]))}]
     (when (seq infractions)
       (for [[identifier params] infractions]
         [base/message
          {:error true
           :content (report-infraction {:identifier identifier
                                        :field field
                                        :value value
                                        :params params})}]))]))

(defn dropdown-input [{::keys [state-key] :as config} field]
  (let [{:keys [valid? value infractions options]} (get-field config field)]
    [:div
     [base/form-dropdown {:error (and (not (nil? valid?)) (not valid?))
                          :label (i18n field)
                          :default-value value
                          :fluid true
                          :selection true
                          :options (map #(set/rename-keys % {:id :value
                                                             :name :text})
                                        options)
                          :on-change (utils/value-handler
                                      #(rf/dispatch [::set-field config field %]))}]
     (when (seq infractions)
       (for [[identifier params] infractions]
         [base/message
          {:error true
           :content (report-infraction {:identifier identifier
                                        :field field
                                        :value value
                                        :params params})}]))]))

(defn valid-form? [config]
  (->> (get-fields config)
       (vals)
       (map :valid?)
       (every? #(not= false %))))

(defn set-field-property! [{::keys [state-key]} field property value]
  (rf/dispatch [:ventas/db [state-key ::fields field property] value]))

(defn get-key [{::keys [state-key]}]
  @(rf/subscribe [:ventas/db [state-key ::population-hash]]))
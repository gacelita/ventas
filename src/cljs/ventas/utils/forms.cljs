(ns ventas.utils.forms
  (:require
   [re-frame.core :as rf]
   [ventas.utils :as utils]
   [ventas.utils.validation :as validation]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

(rf/reg-event-db
 ::populate
 (fn [db [_ {::keys [state-key]} data]]
   (let [last-hash (get-in db [state-key ::population-hash])]
     (if (= last-hash (hash data))
       db
       (->
        (reduce (fn [acc [field value]]
                  (assoc-in acc [state-key field :value] value))
                db
                data)
        (assoc-in [state-key ::population-hash] (hash data)))))))

(rf/reg-event-db
 ::set-field
 (fn [db [_ {::keys [state-key validators]} field value]]
   (let [{:keys [valid? infractions]} (validation/validate validators field value)]
     (assoc-in db [state-key field] {:value value
                                     :valid? valid?
                                     :infractions infractions}))))

(defn- report-infraction [{:keys [identifier field value params]}]
  (apply i18n identifier (vals params)))

(defn- text-input [{::keys [state-key] :as config} field]
  (let [{:keys [valid? value infractions]} @(rf/subscribe [:ventas/db [state-key field]])]
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

(defn valid-form? [data]
  (not (every? clojure.core/identity (map :valid? (vals data)))))
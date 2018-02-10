(ns ventas.components.form
  "Form events mostly"
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-db
 ::set-field
 (fn [db [_ form-path field value]]
   {:pre [(vector? form-path)]}
   (let [field (if-not (sequential? field)
                 [field]
                 field)]
     (assoc-in db
               (concat form-path field)
               value))))

(rf/reg-event-fx
 ::update-field
 (fn [{:keys [db]} [_ form-path field update-fn]]
   (let [field (if-not (sequential? field)
                 [field]
                 field)
         new-value (update-fn (get-in db (concat form-path field)))]
     {:dispatch [::set-field field new-value]})))

(rf/reg-event-db
 ::populate
 (fn [db [_ state-key data]]
   (-> db
       (assoc-in [state-key :form] data)
       (assoc-in [state-key :form-hash] (hash data)))))
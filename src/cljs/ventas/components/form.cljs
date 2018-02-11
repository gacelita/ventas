(ns ventas.components.form
  "Form events mostly"
  (:require
   [re-frame.core :as rf]))

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
     {:dispatch [::set-field field new-value]})))

(rf/reg-event-db
 ::populate
 (fn [db [_ db-path data]]
   {:pre [(vector? db-path)]}
   (-> db
       (assoc-in (conj db-path :form) data)
       (assoc-in (conj db-path :form-hash) (hash data)))))
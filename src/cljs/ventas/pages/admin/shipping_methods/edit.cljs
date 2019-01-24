(ns ventas.pages.admin.shipping-methods.edit
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.common.utils :as common.utils]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(defn- table->prices [form]
  (->> (vals form)
       (mapcat (fn [{:keys [min-value country-groups]}]
                 (map (fn [[group value]]
                        (when-not (str/blank? value)
                          {:schema/type :schema.type/shipping-method.price
                           :shipping-method.price/amount {:schema/type :schema.type/amount
                                                          :amount/value (common.utils/str->bigdec value)
                                                          :amount/currency [:currency/keyword :eur]}
                           :shipping-method.price/country-groups #{group}
                           :shipping-method.price/min-value (common.utils/str->bigdec min-value)}))
                      country-groups)))
       (remove nil?)))

(defn- prices->table [prices]
  (reduce-kv (fn [acc idx [min-value prices]]
               (reduce (fn [acc {:shipping-method.price/keys [country-groups amount]}]
                         (assoc-in acc
                                   [idx
                                    :country-groups
                                    (-> country-groups first :db/id)]
                                   (common.utils/bigdec->str (:amount/value amount))))
                       (assoc-in acc [idx :min-value] min-value)
                       prices))
             {}
             (->> prices
                  (map #(update % :shipping-method.price/min-value common.utils/bigdec->str))
                  (sort-by :shipping-method.price/min-value)
                  (group-by :shipping-method.price/min-value)
                  (into []))))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.entities.save
               {:params (-> (form/get-data db [state-key])
                            (assoc :shipping-method/prices (table->prices (form/get-data db [state-key :prices-table]))))
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
    :go-to [:admin.shipping-methods]}))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [state-key :price-index] 1))
    :dispatch-n [[::events/enums.get :shipping-method.pricing]
                 [::backend/admin.entities.list
                  {:success [::init.groups]
                   :params {:type :country.group}}]
                 (let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/shipping-method}]
                     [::backend/admin.entities.pull
                      {:params {:id id}
                       :success [::init.next]}]))]}))

(rf/reg-event-fx
 ::init.next
 (fn [{:keys [db]} [_ data]]
   {:db (assoc-in db
                  [state-key :price-index]
                  (->> (:shipping-method/prices data)
                       (group-by :shipping-method.price/min-value)
                       (keys)
                       (count)))
    :dispatch-n [[::form/populate [state-key] data]
                 [::form/populate [state-key :prices-table] (prices->table (:shipping-method/prices data))]]}))

(rf/reg-event-fx
 ::init.groups
 (fn [{:keys [db]} [_ groups]]
   {:db (-> db
            (update-in [state-key :prices-table :form 0 :min-value]
                       #(or % "0"))
            (assoc-in [state-key :country.groups] groups))}))

(rf/reg-event-db
 ::update-last-index
 (fn [db [_ index]]
   (let [current-index (get-in db [state-key :price-index])]
     (if (= current-index index)
       (update-in db [state-key :price-index] inc)
       db))))

(rf/reg-event-fx
 ::field-changed
 (fn [_ [_ idx]]
   {:dispatch-n [[::update-last-index idx]]}))

(defn- prices-table []
  (let [db-path [state-key :prices-table]]
    [form/form db-path
     [base/table {:celled true :sortable true :class "admin-shipping-methods-edit__pricings"}
      [base/table-header
       [base/table-row
        [base/table-header-cell]
        (map (fn [idx]
               [base/table-header-cell
                [form/field {:db-path db-path
                             :key [idx :min-value]
                             :placeholder "Minimum"
                             :on-change-fx [::field-changed idx]}]])
             (range (inc @(rf/subscribe [:db [state-key :price-index]]))))]]
      [base/table-body
       (let [rows @(rf/subscribe [:db [state-key :country.groups]])]
         (doall
          (for [{:keys [name id]} rows]
            [base/table-row {:key id}
             [base/table-cell {:key id} name]
             (map (fn [idx]
                    [base/table-cell
                     [form/field {:db-path db-path
                                  :key [idx :country-groups id]
                                  :placeholder "Price"
                                  :on-change-fx [::field-changed idx]}]])
                  (range (inc @(rf/subscribe [:db [state-key :price-index]]))))])))]]]))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])

(defn content []
  [form/form [state-key]
   (let [{{:keys [culture]} :identity} @(rf/subscribe [:db [:session]])]
     [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

      [base/segment {:color "orange"
                     :title "Shipping method"}

       [field {:key :shipping-method/name
               :type :i18n
               :culture culture}]

       [field {:key :shipping-method/default?
               :type :toggle}]

       [field {:key :shipping-method/manipulation-fee
               :type :amount}]

       [field {:key [:shipping-method/pricing :db/id]
               :type :combobox
               :options @(rf/subscribe [:db [:enums :shipping-method.pricing]])}]

       [prices-table]]

      [base/form-button {:type "submit"}
       (i18n ::submit)]])])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-shipping-methods-edit__page
    [content]]])

(routes/define-route!
 :admin.shipping-methods.edit
 {:name ::page
  :url [:id "/edit"]
  :component page
  :init-fx [::init]})

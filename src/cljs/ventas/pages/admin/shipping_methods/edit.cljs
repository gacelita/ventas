(ns ventas.pages.admin.shipping-methods.edit
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.form :as form]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]
   [ventas.common.utils :as common.utils])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(defn- ->prices [prices]
  (mapcat (fn [[_ {:keys [min groups]}]]
            (map (fn [[group value]]
                   {:schema/type :schema.type/shipping-method.price
                    :shipping-method.price/amount {:schema/type :schema.type/amount
                                                   :amount/value value
                                                   :amount/currency [:currency/keyword :eur]}
                    :shipping-method.price/country-groups #{group}
                    :shipping-method.price/min-value min})
                 groups))
          prices))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.entities.save
               {:params (-> (form/get-data db [state-key])
                            (assoc :shipping-method/prices (->prices (get-in db [state-key :prices]))))
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
                       :success [::form/populate [state-key]]}]))]}))

(rf/reg-event-fx
 ::init.groups
 (fn [{:keys [db]} [_ groups]]
   {:db (-> db
            (assoc-in [state-key :prices-table :form (keyword "pricing-0-group-" (first groups))] "0")
            (assoc-in [state-key :country.groups] groups))}))

(rf/reg-event-db
 ::update-last-index
 (fn [db [_ index]]
   (let [current-index (get-in db [state-key :price-index])]
     (if (= current-index index)
       (update-in db [state-key :price-index] inc)
       db))))

(rf/reg-event-db
 ::save-value
 (fn [db [_ {:keys [index group type]} value]]
   (if (= type :minimum)
     (assoc-in db [state-key :prices index :min] value)
     (assoc-in db [state-key :prices index :groups group] value))))

(rf/reg-event-fx
 ::field-changed
 (fn [_ [_ {:keys [index group type]} value]]
   (let [value (common.utils/str->bigdec value)]
     {:dispatch-n [[::update-last-index index]
                   [::save-value {:index index
                                  :group group
                                  :type type} value]]})))

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
                             :type :number
                             :key (keyword (str "pricing-" idx "-min"))
                             :placeholder "Minimum"
                             :on-change-fx [::field-changed {:index idx
                                                             :type :minimum}]}]])
             (range (inc @(rf/subscribe [::events/db [state-key :price-index]]))))]]
      [base/table-body
       (let [rows @(rf/subscribe [::events/db [state-key :country.groups]])]
         (doall
          (for [{:keys [name id]} rows]
            [base/table-row {:key id}
             [base/table-cell {:key id} name]
             (map (fn [idx]
                    [base/table-cell
                     [form/field {:db-path db-path
                                  :type :number
                                  :key (keyword (str "pricing-" idx "-group-" id))
                                  :placeholder "Price"
                                  :on-change-fx [::field-changed {:index idx
                                                                  :group id
                                                                  :type :value}]}]])
                  (range (inc @(rf/subscribe [::events/db [state-key :price-index]]))))])))]]]))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])

(defn content []
  [:div
   [form/form [state-key]
    (let [{{:keys [culture]} :identity} @(rf/subscribe [::events/db [:session]])]
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
                :options @(rf/subscribe [::events/db [:enums :shipping-method.pricing]])}]

        [prices-table]]

       [base/form-button {:type "submit"}
        (i18n ::submit)]])]])

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

(ns ventas.pages.admin.orders.edit
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.components.base :as base]
   [ventas.components.form :as form]
   [ventas.components.notificator :as notificator]
   [ventas.components.table :as table]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.common :as admin.common]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]
   [ventas.common.utils :as common.utils])
  (:require-macros
   [ventas.utils :refer [ns-kw]]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} _]
   {:dispatch [::backend/admin.entities.save
               {:params (get-in db [state-key :form])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [_ _]
   {:dispatch [::notificator/notify-saved]
    :go-to [:admin.orders]}))

(defn- on-user-change [user]
  [::backend/admin.entities.list
   {:params {:filters {:user user}
             :type :address}
    :success [::events/db [state-key :user-addresses]]}])

(defn- image-column [row]
  (when-let [image (first (get-in row [:product-variation :images]))]
    [:img {:key (:id image)
           :src (str "/images/" (:id image) "/resize/admin-orders-edit-line")}]))

(rf/reg-sub
 ::user-addresses
 (fn [_]
   (rf/subscribe [::events/db [state-key :user-addresses]]))
 (fn [addresses]
   (map (fn [address]
          {:text (reagent/as-element
                  [:div
                   [:p (:address address)]
                   [:p (str/join ", " [(:city address)
                                       (get-in address [:state :name])
                                       (get-in address [:country :name])])]])
           :value (:id address)})
        addresses)))

(rf/reg-sub
 ::shipping-methods
 (fn [db]
   (get-in db [state-key :shipping-methods])))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::table/init [state-key :lines-table]
                  {:items-per-page 5
                   :columns [{:id :image
                              :label (i18n ::image)
                              :component image-column}
                             {:id :name
                              :label (i18n ::name)
                              :component (fn [row] [:span (get-in row [:product-variation :name])])}
                             {:id :price
                              :label (i18n ::price)
                              :component (fn [row] [:span (common.utils/bigdec->str (get-in row [:product-variation :price :value]))])}
                             {:id :quantity
                              :label (i18n ::quantity)}
                             {:id :total
                              :label (i18n ::total)
                              :component (fn [row] [:span (* (:quantity row)
                                                             (common.utils/bigdec->str (get-in row [:product-variation :price :value])))])}]}]
                 (let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/order}]
                     [::backend/admin.orders.get
                      {:params {:id id}
                       :success ::init.next}]))
                 [::events/enums.get :order.status]
                 [::backend/admin.entities.list
                  {:params {:type :shipping-method}
                   :success [::events/db [state-key :shipping-methods]]}]]}))

(rf/reg-event-fx
 ::init.next
 (fn [{:keys [db]} [_ {:keys [order lines status-history]}]]
   {:dispatch-n [[::form/populate [state-key] order]
                 [::backend/admin.entities.find-serialize
                  {:params {:id (get-in order [:order/user :db/id])}
                   :success [::events/db [state-key :user]]}]]
    :db (-> db
            (assoc-in [state-key :lines-table :rows] lines)
            (assoc-in [state-key :status-history] status-history))}))

(defn- field [{:keys [key] :as args}]
  [form/field (merge args
                     {:db-path [state-key]
                      :label (i18n (ns-kw (if (sequential? key)
                                            (first key)
                                            key)))})])

(defn- content []
  [form/form [state-key]
   [base/form {:on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

    [base/segment {:color "orange"
                   :title (i18n ::order)}

     [admin.common/entity-search-field
      {:label (i18n ::user)
       :db-path [state-key]
       :key [:order/user :db/id]
       :attrs #{:user/first-name
                :user/last-name}
       :selected-option @(rf/subscribe [::events/db [state-key :user]])}]

     [field {:key [:order/status :db/id]
             :type :combobox
             :options @(rf/subscribe [::events/db [:enums :order.status]])}]

     [:h5 (i18n ::status-history)]
     (let [lines @(rf/subscribe [::events/db [state-key :status-history]])]
       (if (empty? lines)
         [:p (i18n ::nothing-yet)]
         [base/table
          [base/table-header
           [base/table-row
            [base/table-header-cell (i18n ::status)]
            [base/table-header-cell (i18n ::date)]]]
          [base/table-body
           (doall
            (for [{:keys [status date]} lines]
              [base/table-row
               [base/table-cell (i18n status)]
               [base/table-cell (.format (js/moment date) "dddd, MMMM Do YYYY, h:mm:ss a")]]))]]))]

    [base/segment {:color "orange"
                   :title (i18n ::billing)}

     ;; @TODO Do this
     [field {:key :order/payment-method
             :disabled true
             :type :text}]

     [field {:key :order/billing-address
             :type :combobox
             :options @(rf/subscribe [::user-addresses])}]

     [field {:key :order/payment-reference
             :type :text}]]

    [base/divider {:hidden true}]

    [base/segment {:color "orange"
                   :title (i18n ::shipping)}

     [field {:key :order/shipping-method
             :type :entity
             :xform {:in :db/id
                     :out (fn [v] {:db/id v})}
             :options (map admin.common/entity->option @(rf/subscribe [::shipping-methods]))}]

     [field {:key :order/shipping-address
             :type :combobox
             :options @(rf/subscribe [::user-addresses])}]

     [field {:key :order/shipping-comments
             :type :textarea}]]

    [base/divider {:hidden true}]

    [base/segment {:color "orange"
                   :title (i18n ::lines)}

     [table/table [state-key :lines-table]]]

    [base/form-button {:type "submit"} (i18n ::submit)]]])

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-orders-edit__page
    [content]]])

(routes/define-route!
  :admin.orders.edit
  {:name ::page
   :url [:id "/edit"]
   :component page
   :init-fx [::init]})

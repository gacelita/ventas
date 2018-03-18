(ns ventas.pages.admin.orders.edit
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.table :as table]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [ventas.utils.ui :as utils.ui]
   [reagent.core :as reagent]
   [ventas.components.form :as form]
   [ventas.pages.admin.common :as admin.common])
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

(defn- lines-table []
  [table/table
   {:init-state {:page 0
                 :items-per-page 5
                 :sort-column :id
                 :sort-direction :asc}
    :state-path [state-key :lines-table]
    :data-path [state-key :lines]
    :columns [{:id :image
               :label (i18n ::image)
               :component image-column}
              {:id :name
               :label (i18n ::name)
               :component (fn [row] [:span (get-in row [:product-variation :name])])}
              {:id :price
               :label (i18n ::price)
               :component (fn [row] [:span (get-in row [:product-variation :price :value])])}
              {:id :quantity
               :label (i18n ::quantity)}
              {:id :total
               :label (i18n ::total)
               :component (fn [row] [:span (* (:quantity row)
                                              (get-in row [:product-variation :price :value]))])}]}])

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

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [(let [id (routes/ref-from-param :id)]
                   (if-not (pos? id)
                     [::form/populate [state-key] {:schema/type :schema.type/order}]
                     [::backend/admin.orders.get
                      {:params {:id id}
                       :success ::init.next}]))
                 [::events/enums.get :order.status]]}))

(rf/reg-event-fx
 ::init.next
 (fn [{:keys [db]} [_ {:keys [order lines]}]]
   {:dispatch-n [[::form/populate [state-key] order]
                 [::backend/admin.entities.find-serialize
                  {:params {:id (get-in order [:order/user :db/id])}
                   :success [::events/db [state-key :user]]}]]
    :db (assoc-in db [state-key :lines] lines)}))

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
             :options @(rf/subscribe [::events/db [:enums :order.status]])}]]

    [base/segment {:color "orange"
                   :title (i18n ::billing)}

     ;; @TODO Do this
     [field {:key :order/payment-method
             :type :text}]

     [field {:key :order/billing-address
             :type :combobox
             :options @(rf/subscribe [::user-addresses])}]

     [field {:key :order/payment-reference
             :type :text}]]

    [base/divider {:hidden true}]

    [base/segment {:color "orange"
                   :title (i18n ::shipping)}

     ;; @TODO Do this
     [field {:key :order/shipping-method
             :type :text}]

     [field {:key :order/shipping-address
             :type :combobox
             :options @(rf/subscribe [::user-addresses])}]

     [field {:key :order/shipping-comments
             :type :textarea}]]

    [base/divider {:hidden true}]

    [base/segment {:color "orange"
                   :title (i18n ::lines)}

     [lines-table]]

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

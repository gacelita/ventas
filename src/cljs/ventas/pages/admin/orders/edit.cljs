(ns ventas.pages.admin.orders.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.i18n :refer [i18n]]
   [ventas.common.utils :as common.utils]
   [ventas.components.notificator :as notificator]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [clojure.string :as str]
   [ventas.components.table :as table]))

(def state-key ::state)

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} [_ data]]
   {:dispatch [::backend/admin.orders.save
               {:params (get-in db [state-key :order])
                :success ::submit.next}]}))

(rf/reg-event-fx
 ::submit.next
 (fn [cofx [_ data]]
   {:dispatch [::notificator/add {:message (i18n ::order-saved-notification)
                                  :theme "success"}]
    :go-to [:admin.orders]}))

(defn- on-user-change [user]
  [::backend/admin.users.addresses.list
   {:params {:id user}
    :success [::events/db [state-key :user-addresses]]}])

(defmulti on-change (fn [_ k v] k))

(defmethod on-change :order/user [_ _ v]
  {:dispatch (on-user-change v)})

(defmethod on-change :default [_ _ _])

(rf/reg-event-fx
 ::on-change
 (fn [cofx [_ k v]]
   (on-change cofx k v)))

(rf/reg-event-fx
  ::set-field
  (fn [{:keys [db]} [_ k v]]
    (merge
     {:db (assoc-in db [state-key :order k] v)}
     (when v
       {:dispatch [::on-change k v]}))))

(rf/reg-event-fx
 ::line-init
 (fn [{:keys [db]} [_ line]]
   (when line
     (let [{:keys [initiated?]} (get-in db [state-key :lines line])]
       (when (not initiated?)

         )))))

(defn- image-column [row]
  (when-let [image (first (get-in row [:product-variation :images]))]
    [:img {:key (:id image)
           :src (str "/images/" (:id image) "/resize/admin-orders-edit-line")}]))

(defn- lines-table [{:keys [product-variation quantity]}]
  [table/table
   {:init-state {:page 0
                 :items-per-page 5
                 :sort-column :id
                 :sort-direction :asc}
    :state-path [state-key :lines-table]
    :data-path [state-key :order-lines]
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

(rf/reg-event-fx
 ::init
 (fn [cofx [_ id]]
   {:dispatch [::backend/admin.orders.get
               {:params {:id id}
                :success ::init.next}]}))

(rf/reg-event-fx
 ::init.next
 (fn [{:keys [db]} [_ {:keys [order lines]}]]
   (merge
    {:db (-> db
             (assoc-in [state-key :order] order)
             (assoc-in [state-key :order-lines] lines))}
    (when-let [user (:order/user order)]
      {:dispatch (on-user-change user)}))))

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

(defn- content []
  (rf/dispatch [::init (-> (routes/current)
                           (get-in [:route-params :id])
                           (js/parseInt))])
  (rf/dispatch [::events/enums.get :order.status])
  (rf/dispatch [::backend/admin.users.list
                {:success [::events/db [state-key :users]]}])
  (fn []
    (let [{:keys [order]} @(rf/subscribe [::events/db state-key])]

      [base/form {:key (:db/id order)
                  :on-submit (utils.ui/with-handler #(rf/dispatch [::submit]))}

       [base/segment {:color "orange"
                      :title (i18n ::order)}

        (let [field :order/user]
          [base/form-field
           [:label (i18n ::user)]
           [base/dropdown
            {:fluid true
             :selection true
             :options (->> @(rf/subscribe [::events/db [state-key :users]])
                           (map (fn [{:keys [id first-name last-name]}]
                                  {:value id
                                   :text (str first-name
                                              (when last-name
                                                (str " " last-name)))})))
             :default-value (get order field)
             :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])

        (let [field :order/status]
          [base/form-field
           [:label (i18n ::status)]
           [base/dropdown
            {:options @(rf/subscribe [::events/db [:enums :order.status]])
             :selection true
             :default-value (get order field)
             :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])]

       [base/segment {:color "orange"
                      :title (i18n ::billing)}
        (let [field :order/payment-method]
          [base/form-input
           {:label (i18n ::payment-method)
            :default-value (get order field)
            :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])

        (let [field :order/billing-address]
          [base/form-field
           [:label (i18n ::billing-address)]
           [base/dropdown
            {:options @(rf/subscribe [::user-addresses])
             :selection true
             :default-value (get order field)
             :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])

        (let [field :order/payment-reference]
          [base/form-input
           {:label (i18n ::payment-reference)
            :default-value (get order field)
            :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])]

       [base/divider {:hidden true}]

       [base/segment {:color "orange"
                      :title (i18n ::shipping)}
        (let [field :order/shipping-method]
          [base/form-input
           {:label (i18n ::shipping-method)
            :default-value (get order field)
            :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])

        (let [field :order/shipping-address]
          [base/form-field
           [:label (i18n ::shipping-address)]
           [base/dropdown
            {:options @(rf/subscribe [::user-addresses])
             :selection true
             :default-value (get order field)
             :on-change #(rf/dispatch [::set-field field (.-value %2)])}]])

        (let [field :order/shipping-comments]
          [base/form-textarea
           {:label (i18n ::shipping-comments)
            :default-value (get order field)
            :on-change #(rf/dispatch [::set-field field (-> % .-target .-value)])}])]

       [base/divider {:hidden true}]

       [base/segment {:color "orange"
                      :title (i18n ::lines)}
        [lines-table]]

       [base/form-button {:type "submit"} (i18n ::submit)]])))

(defn page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-orders-edit__page
    [content]]])

(routes/define-route!
 :admin.orders.edit
 {:name ::page
  :url [:id "/edit"]
  :component page})
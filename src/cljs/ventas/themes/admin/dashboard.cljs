(ns ventas.themes.admin.dashboard
  (:require
   [moment]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.notificator :as notificator]
   [ventas.components.table :as table]
   [ventas.server.api.admin :as api.admin]
   [ventas.i18n :refer [i18n]]
   [ventas.themes.admin.orders.edit :as admin.orders.edit]
   [ventas.themes.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.utils.formatting :as utils.formatting]))

(def state-key ::state)

(rf/reg-event-fx
 ::pending-orders.init
 (fn [_ _]
   {:dispatch [::api.admin/admin.orders.list-pending
               {:success [:db [state-key :pending-orders]]}]}))

(defn- segment [{:keys [label]} & children]
  [base/grid-column
   [base/segment
    [base/header {:as "h3"}
     label]
    children]])

(defn- user-view [{:keys [id first-name last-name created-at]}]
  [:li.admin-dashboard__user
   [:a {:href (routes/path-for :admin.users.edit :id id)}
    [:p (if (or first-name last-name)
          (str/join " " [first-name last-name])
          (i18n ::no-name))]
    [:p (utils.formatting/format-date created-at)]]])

(defn- latest-users []
  (let [{:keys [users]} @(rf/subscribe [:db [state-key]])]
    [:ul.admin-dashboard__users
     (doall
      (for [user users]
        ^{:key (:id user)}
        [user-view user]))]))

(defn- pending-order-button [attrs text]
  [base/button (merge {:size "mini"
                       :class "pending-orders__button"}
                      attrs)
   text])

(rf/reg-event-fx
 ::order.set-status
 (fn [_ [_ id status]]
   {:dispatch [::api.admin/admin.entities.save
               {:params {:db/id id
                         :schema/type :schema.type/order
                         :order/status status}
                :success [::order.set-status.next]}]}))

(rf/reg-event-fx
 ::order.set-status.next
 (fn [_ _]
   {:dispatch-n [[::notificator/notify-saved nil]
                 [::pending-orders.init]]}))

(rf/reg-event-db
 ::modal.toggle
 (fn [db [_ content]]
   (-> db
       (update-in [state-key :order-modal :visible?] not)
       (assoc-in [state-key :order-modal :content] content))))

(rf/reg-event-fx
 ::modal.init
 (fn [_ [_ id modal-content]]
   {:dispatch [::api.admin/admin.orders.get
               {:params {:id id}
                :success [::modal.init.next modal-content]}]}))

(rf/reg-event-fx
 ::modal.init.next
 (fn [_ [_ modal-content {:keys [lines] :as order}]]
   {:dispatch-n [[:db [state-key :order] order]
                 [::table/init [state-key :order-lines-table]
                  (merge admin.orders.edit/table-config
                         {:rows lines})]
                 [::modal.toggle modal-content]]}))

(defn modal []
  (let [{:keys [visible? content]} @(rf/subscribe [:db [state-key :order-modal]])]
    [base/modal {:size "small"
                 :open visible?
                 :on-close #(rf/dispatch [::modal.toggle])}
     [base/modal-content
      (when content
        [content])]]))

(defn payment-info-modal []
  (let [data @(rf/subscribe [:db [state-key :order]])]
    [:div
     [base/header "Payment info"]
     [:p "Payment method: " (i18n (keyword "payment-method" (get-in data [:order :order/payment-method])))]]))

(defn order-items-modal []
  [:div
   [base/header "Order items"]
   [table/table [state-key :order-lines-table]]])

(defn shipping-info-modal []
  (let [{:keys [method address]} (:shipping @(rf/subscribe [:db [state-key :order]]))]
    [:div
     [base/header "Shipping info"]
     [:p [:strong "Method: "] (:name method)]
     [:p [:strong "Address: "]
      [:div.pending-orders__address
       [:p (:first-name address) " " (:last-name address)]
       [:p (:address address) " " (:address-second-line address)]
       [:p (:zip address) " " (:city address) " " (:name (:state address))]
       [:p (:name (:country address))]]]]))

(defn- pending-orders []
  [base/table {:class "pending-orders" :unstackable true}
   [base/table-header
    [base/table-row
     [base/table-header-cell
      [:span "ID"]]
     [base/table-header-cell
      [:span "Amount"]]
     [base/table-header-cell
      [:span "Date"]]
     [base/table-header-cell
      [:span "Status"]]]]
   [base/table-body
    (let [orders @(rf/subscribe [:db [state-key :pending-orders]])]
      (if (empty? orders)
        [base/table-row
         [base/table-cell {:col-span 4}
          [:p.table-component__no-rows (i18n ::table/no-rows)]]]
        (for [order orders]
          [base/table-row {:class (str "pending-orders__order "
                                       "pending-orders__order--" (name (:status order)))}
           [base/table-cell
            [:a {:href (routes/path-for :admin.orders.edit :id (:id order))} [:p (:id order)]]
            (case (:status order)
              :order.status/unpaid [:div
                                    [pending-order-button
                                     {:on-click #(rf/dispatch [::modal.init (:id order) #'payment-info-modal])}
                                     "See payment info"]
                                    [pending-order-button
                                     {:on-click #(rf/dispatch [::order.set-status (:id order) :order.status/paid])}
                                     "Set as paid"]]
              :order.status/paid [:div
                                  [pending-order-button
                                   {:on-click #(rf/dispatch [::modal.init (:id order) #'order-items-modal])}
                                   "See order items"]
                                  [pending-order-button
                                   {:on-click #(rf/dispatch [::order.set-status (:id order) :order.status/acknowledged])}
                                   "Set as acknowledged"]]
              :order.status/acknowledged [:div
                                          [pending-order-button
                                           {:on-click #(rf/dispatch [::modal.init (:id order) #'order-items-modal])}
                                           "See order items"]
                                          [pending-order-button
                                           {:on-click #(rf/dispatch [::order.set-status (:id order) :order.status/ready])}
                                           "Set as ready"]]
              :order.status/ready [:div
                                   [pending-order-button
                                    {:on-click #(rf/dispatch [::modal.init (:id order) #'shipping-info-modal])}
                                    "See shipping info"]
                                   [pending-order-button
                                    {:on-click #(rf/dispatch [::order.set-status (:id order) :order.status/shipped])}
                                    "Set as shipped"]]
              "")]
           [base/table-cell (utils.formatting/amount->str (:amount order))]
           [base/table-cell (utils.formatting/format-date (:created-at order))]
           [base/table-cell (i18n (:status order))]])))]])

(defn- content []
  [base/grid {:stackable true :columns 2}
   [base/grid-column
    [base/grid-row
     [segment {:label (i18n ::pending-orders)}
      [pending-orders]]]
    [:br]
    [base/grid-row
     [segment {:label (i18n ::latest-users)}
      [latest-users]]]]])

(defn- page []
  [admin.skeleton/skeleton
   [:div.admin__default-content.admin-dashboard__page
    [content]
    [modal]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::api.admin/admin.users.list
                  {:success [:db [state-key :users]]}]
                 [::pending-orders.init]]}))
(ns ventas.pages.datadmin
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.page :refer [pages]]
   [ventas.routes :refer [route-parents routes]]
   [ventas.components.notificator]
   [ventas.components.popup]
   [ventas.components.product-list :refer [products-list]]
   [ventas.components.cart :as cart]
   [ventas.utils :as util]
   [ventas.components.base :as base]
   [ventas.routes :as routes]
   [ventas.events :as events]))

(defn skeleton [contents]
  (info "Rendering...")
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    [:div.ventas.root
      [ventas.components.notificator/notificator]
      [ventas.components.popup/popup]
      [:div.ventas.wrapper
        [base/container {:class "bu main"}
          [base/divider]
          ^{:key current-page} contents]]]))

(rf/reg-sub :datadmin/datoms
  (fn [db _] (-> db :datadmin :datoms)))

(rf/reg-sub :datadmin/filters
  (fn [db _] (-> db :datadmin :filters)))

(rf/reg-event-db :datadmin.filters/add
  (fn [db [_ field value]]
    (-> db (update-in [:datadmin :filters] assoc field value))))

(rf/reg-event-db :datadmin.filter/remove
  (fn [db [_ field value]]
    (-> db (update-in [:datadmin :filters] dissoc field))))

(rf/reg-event-fx :datadmin/datoms
  (fn [cofx [_]]
    {:ws-request {:name :datadmin/datoms
                  :params {}
                  :success #(rf/dispatch [::events/db [:datadmin :datoms] (:datoms %)])}}))

(defn page []
  (rf/dispatch [:datadmin/datoms])
  (fn []
    [skeleton
      [:div
        [:h2 "Datadmin"]
        [base/table {:celled true}
          [base/tableHeader
           [base/tableRow
            [base/tableHeaderCell "Entity"]
            [base/tableHeaderCell "Attribute"]
            [base/tableHeaderCell "Value"]
            [base/tableHeaderCell "Transaction"]]]
          [base/tableBody
           (for [datom @(rf/subscribe [:datadmin/datoms])]
             [base/tableRow
              [base/tableCell (:e datom)]
              [base/tableCell (:a datom)]
              [base/tableCell (:v datom)]
              [base/tableCell [:a {:on-click #()} (:tx datom)] ]])]
           [base/tableFooter
            [base/tableRow
             [base/tableHeaderCell {:colSpan "4"}
              [base/menu {:floated "right" :pagination true}
               [base/menuItem {:as "a" :icon true}
                [base/icon {:name "left chevron"}]]
               [base/menuItem {:as "a"} "1"]
               [base/menuItem {:as "a"} "2"]
               [base/menuItem {:as "a"} "3"]
               [base/menuItem {:as "a"} "4"]
               [base/menuItem {:as "a" :icon true}
                [base/icon {:name "right chevron"}]]
               ]]]]]
        [:h3 "Filters"]

        [base/table {:celled true}
          [base/tableHeader
            [base/tableRow
              [base/tableHeaderCell "Field"]
              [base/tableHeaderCell "Value"]]]
          [base/tableBody
            (for [filter @(rf/subscribe [:datadmin/filters])]
              [base/tableRow
                [base/tableCell
                  [base/select {:placeholder "Field"
                             :options (clj->js [{:value :e :text "Entity"}
                                                {:value :a :text "Attribute"}
                                                {:value :v :text "Value"}
                                                {:value :t :text "Transaction"}
                                                ])}]]
                [base/tableCell
                  [base/input {:placeholder "Value"
                             :type :text}]
                  [base/button "Delete"]]])
             [base/tableRow
              [base/tableCell
                [base/select {:placeholder "Field"
                            :options (clj->js [{:value :e :text "Entity"}
                                               {:value :a :text "Attribute"}
                                               {:value :v :text "Value"}
                                               {:value :t :text "Transaction"}
                                              ])}]]
              [base/tableCell
                [base/input {:placeholder "Value"
                           :type :text}]
                [base/button "Add" {:on-click #(rf/dispatch [:datadmin.filters/add ])}]]]]]

        ]]))


(routes/define-route!
 :datadmin
 {:name "Datadmin"
  :url "datadmin"
  :component page})
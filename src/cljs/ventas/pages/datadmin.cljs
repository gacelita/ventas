(ns ventas.pages.datadmin
  (:require [reagent.core :as reagent :refer [atom]]
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
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]
            [ventas.routes :as routes]))

(defn skeleton [contents]
  (info "Rendering...")
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    [:div.ventas.root
      [ventas.components.notificator/notificator]
      [ventas.components.popup/popup]
      [:div.ventas.wrapper
        [sa/Container {:class "bu main"}
          [sa/Divider]
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
                  :success-fn #(rf/dispatch [:ventas/db [:datadmin :datoms] (:datoms %)])}}))

(defn page []
  (rf/dispatch [:datadmin/datoms])
  (fn []
    [skeleton
      [:div
        [:h2 "Datadmin"]
        [sa/Table {:celled true}
          [sa/TableHeader
           [sa/TableRow
            [sa/TableHeaderCell "Entity"]
            [sa/TableHeaderCell "Attribute"]
            [sa/TableHeaderCell "Value"]
            [sa/TableHeaderCell "Transaction"]]]
          [sa/TableBody
           (for [datom @(rf/subscribe [:datadmin/datoms])]
             [sa/TableRow
              [sa/TableCell (:e datom)]
              [sa/TableCell (:a datom)]
              [sa/TableCell (:v datom)]
              [sa/TableCell [:a {:on-click #()} (:tx datom)] ]])]
           [sa/TableFooter
            [sa/TableRow
             [sa/TableHeaderCell {:colSpan "4"}
              [sa/Menu {:floated "right" :pagination true}
               [sa/MenuItem {:as "a" :icon true}
                [sa/Icon {:name "left chevron"}]]
               [sa/MenuItem {:as "a"} "1"]
               [sa/MenuItem {:as "a"} "2"]
               [sa/MenuItem {:as "a"} "3"]
               [sa/MenuItem {:as "a"} "4"]
               [sa/MenuItem {:as "a" :icon true}
                [sa/Icon {:name "right chevron"}]]
               ]]]]]
        [:h3 "Filters"]

        [sa/Table {:celled true}
          [sa/TableHeader
            [sa/TableRow
              [sa/TableHeaderCell "Field"]
              [sa/TableHeaderCell "Value"]]]
          [sa/TableBody
            (for [filter @(rf/subscribe [:datadmin/filters])]
              [sa/TableRow
                [sa/TableCell
                  [sa/Select {:placeholder "Field"
                             :options (clj->js [{:value :e :text "Entity"}
                                                {:value :a :text "Attribute"}
                                                {:value :v :text "Value"}
                                                {:value :t :text "Transaction"}
                                                ])}]]
                [sa/TableCell
                  [sa/Input {:placeholder "Value"
                             :type :text}]
                  [sa/Button "Delete"]]])
             [sa/TableRow
              [sa/TableCell
                [sa/Select {:placeholder "Field"
                            :options (clj->js [{:value :e :text "Entity"}
                                               {:value :a :text "Attribute"}
                                               {:value :v :text "Value"}
                                               {:value :t :text "Transaction"}
                                              ])}]]
              [sa/TableCell
                [sa/Input {:placeholder "Value"
                           :type :text}]
                [sa/Button "Add" {:on-click #(rf/dispatch [:datadmin.filters/add ])}]]]]]

        ]]))


(routes/define-route!
 :datadmin
 {:name "Datadmin"
  :url "datadmin"
  :component page})
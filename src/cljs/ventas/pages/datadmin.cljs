(ns ventas.pages.datadmin
  "Underdeveloped idea; don't use, don't read, etc."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.components.notificator]
   [ventas.components.popup]
   [ventas.events :as events]
   [ventas.routes :as routes]
   [ventas.utils :as util]
   [ventas.utils.logging :refer [debug error info trace warn]]))

(def state-key ::state)

(defn skeleton [contents]
  (let [handler (routes/handler)]
    [:div.root
     [ventas.components.notificator/notificator]
     [ventas.components.popup/popup]
     [:div.wrapper
      [base/container {:class "bu main"}
       [base/divider]
       ^{:key handler} contents]]]))

(rf/reg-event-db
 ::filters.add
 (fn [db [_ field value]]
   (-> db (update-in [state-key :filters] assoc field value))))

(rf/reg-event-db
 ::filters.remove
 (fn [db [_ field value]]
   (-> db (update-in [state-key :filters] dissoc field))))

(rf/reg-event-fx
 ::datoms.fetch
 (fn [cofx [_]]
   {:ws-request {:name :admin.datadmin.datoms
                 :params {}
                 :success #(rf/dispatch [::events/db [state-key :datoms] (:datoms %)])}}))

(defn page []
  (rf/dispatch [::datoms.fetch])
  (fn []
    [skeleton
     [:div
      [:h2 "Datadmin"]
      [base/table {:celled true}
       [base/table-header
        [base/table-row
         [base/table-header-cell "Entity"]
         [base/table-header-cell "Attribute"]
         [base/table-header-cell "Value"]
         [base/table-header-cell "Transaction"]]]
       [base/table-body
        (for [datom @(rf/subscribe [::events/db [state-key :datoms]])]
          [base/table-row
           [base/table-cell (:e datom)]
           [base/table-cell (:a datom)]
           [base/table-cell (:v datom)]
           [base/table-cell [:a {:on-click #()} (:tx datom)]]])]
       [base/table-footer
        [base/table-row
         [base/table-header-cell {:colSpan "4"}
          [base/menu {:floated "right" :pagination true}
           [base/menu-item {:as "a" :icon true}
            [base/icon {:name "left chevron"}]]
           [base/menu-item {:as "a"} "1"]
           [base/menu-item {:as "a"} "2"]
           [base/menu-item {:as "a"} "3"]
           [base/menu-item {:as "a"} "4"]
           [base/menu-item {:as "a" :icon true}
            [base/icon {:name "right chevron"}]]]]]]]
      [:h3 "Filters"]

      [base/table {:celled true}
       [base/table-header
        [base/table-row
         [base/table-header-cell "Field"]
         [base/table-header-cell "Value"]]]
       [base/table-body
        (for [filter @(rf/subscribe [::events/db [state-key :filters]])]
          [base/table-row
           [base/table-cell
            [base/select {:placeholder "Field"
                          :options (clj->js [{:value :e :text "Entity"}
                                             {:value :a :text "Attribute"}
                                             {:value :v :text "Value"}
                                             {:value :t :text "Transaction"}])}]]
           [base/table-cell
            [base/input {:placeholder "Value"
                         :type :text}]
            [base/button "Delete"]]])
        [base/table-row
         [base/table-cell
          [base/select {:placeholder "Field"
                        :options (clj->js [{:value :e :text "Entity"}
                                           {:value :a :text "Attribute"}
                                           {:value :v :text "Value"}
                                           {:value :t :text "Transaction"}])}]]
         [base/table-cell
          [base/input {:placeholder "Value"
                       :type :text}]
          [base/button "Add" {:on-click #(rf/dispatch [::filters.add])}]]]]]]]))

(routes/define-route!
  :datadmin
  {:name "Datadmin"
   :url "datadmin"
   :component page})

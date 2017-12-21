(ns ventas.pages.api
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.components.notificator]
   [ventas.components.popup]
   [ventas.components.product-list :refer [products-list]]
   [ventas.components.cart :as cart]
   [ventas.utils :as util]
   [soda-ash.core :as sa]))

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

(defn page []
  [skeleton
   [:div
    [:h2 "API tool"]
    [base/table {:celled true}
     [base/table-header
      [base/table-row
       [base/table-header-cell "Entity"]
       [base/table-header-cell "Attribute"]
       [base/table-header-cell "Value"]
       [base/table-header-cell "Transaction"]]]
     [base/table-body]
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
          [base/icon {:name "right chevron"}]]
         ]]]]]
    [:h3 "Filters"]

    [base/table {:celled true}
     [base/table-header
      [base/table-row
       [base/table-header-cell "Field"]
       [base/table-header-cell "Value"]]]
     [base/table-body
      [base/table-row
       [base/table-cell
        [base/select {:placeholder "Field"
                    :options (clj->js [{:value :e :text "Entity"}
                                       {:value :a :text "Attribute"}
                                       {:value :v :text "Value"}
                                       {:value :t :text "Transaction"}
                                       ])}]]
       [base/table-cell
        [base/input {:placeholder "Value"
                   :type :text}]
        [base/button "Add"]]]]]

    ]])

(routes/define-route!
 :api
 {:name "API tool"
  :url "api"
  :component page})
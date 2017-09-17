(ns ventas.pages.api
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.utils.logging :refer [trace debug info warn error]]
            [ventas.page :refer [pages]]
            [ventas.routes :as routes]
            [ventas.components.notificator]
            [ventas.components.popup]
            [ventas.components.product-list :refer [products-list]]
            [ventas.components.cart :as cart]
            [ventas.util :as util]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]))

(routes/define-route!
 {:route :api
  :name "API tool"
  :url "api"})

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

(defmethod pages :api []
  (fn []
    [skeleton
      [:div
        [:h2 "API tool"]
        [sa/Table {:celled true}
          [sa/TableHeader
           [sa/TableRow
            [sa/TableHeaderCell "Entity"]
            [sa/TableHeaderCell "Attribute"]
            [sa/TableHeaderCell "Value"]
            [sa/TableHeaderCell "Transaction"]]]
          [sa/TableBody]
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
                [sa/Button "Add" ]]]]]

        ]]))


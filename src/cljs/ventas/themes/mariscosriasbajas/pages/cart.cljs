(ns ventas.themes.mariscosriasbajas.pages.cart
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.page :refer [pages]]
   [ventas.routes :refer [route-parents routes]]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]
   [ventas.components.category-list :refer [category-list]]
   [ventas.components.product-list :refer [products-list]]
   [ventas.components.cart :as ventas.cart]
   [ventas.themes.mariscosriasbajas.components.header :refer [header]]
   [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]
   [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
   [ventas.themes.mariscosriasbajas.components.heading :as theme.heading]
   [ventas.util :as util]
   [ventas.plugin :as plugin]
   [soda-ash.core :as sa]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

(def products
  [{:id 17592186046432
    :quantity 2}
   {:id 17592186046428
    :quantity 3}
   {:id 17592186046424
    :quantity 1}
   {:id 17592186046160
    :quantity 5}])

(defn line [data]
  (rf/dispatch [:ventas/entities.sync (:id data)])
  (fn [data]
    (let [product @(rf/subscribe [:ventas/db [:entities (:id data)]])]
      [base/tableRow
       [base/tableCell
        [:a {:href (routes/path-for :frontend.product :id (:id product))} (:name product)]]
       [base/tableCell (:description product)]
       [base/tableCell (:price product)]
       [base/tableCell (:quantity data)]
       [base/tableCell (* (:quantity data) (:price product))]])))

(defmethod pages :frontend.cart []
  [skeleton
   [base/container
    [:div.cart-page
     [:h2 (i18n ::cart)]
     [base/table {:celled true :striped true}
      [base/tableHeader
       [base/tableRow
        [base/tableHeaderCell (i18n ::product)]
        [base/tableHeaderCell (i18n ::description)]
        [base/tableHeaderCell (i18n ::price)]
        [base/tableHeaderCell (i18n ::quantity)]
        [base/tableHeaderCell (i18n ::total)]]]
      [base/tableBody
       (map-indexed (fn [idx data] [line data])
                    products)]]]]])
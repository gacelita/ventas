(ns ventas.themes.clothing.pages.frontend.cart
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
   [ventas.themes.clothing.components.header :refer [header]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.themes.clothing.components.preheader :refer [preheader]]
   [ventas.themes.clothing.components.heading :as theme.heading]
   [ventas.utils :as util]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events :as events]))

(defn line [data]
  (rf/dispatch [::events/entities.sync (:id data)])
  (fn [data]
    (when-let [product @(rf/subscribe [::events/db [:entities (:id data)]])]
      [base/tableRow
       [base/tableCell
        [:a {:href (routes/path-for :frontend.product :id (:id product))} (:name product)]]
       [base/tableCell (:description product)]
       [base/tableCell (:price product)]
       [base/tableCell (:quantity data)]
       [base/tableCell (* (:quantity data) (:price product))]])))

(defn page []
  [skeleton
   [base/container
    [:div.cart-page
     [:h2 (i18n ::cart)]
     ]]])

(routes/define-route!
 :frontend.cart
 {:name ::page
  :url ["login"]
  :component page})
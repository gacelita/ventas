(ns ventas.pages.frontend
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [taoensso.timbre :as timbre :refer-macros [trace debug info warn error]]
            [ventas.page :refer [pages]]
            [ventas.routes :refer [route-parents routes]]
            [ventas.components.notificator]
            [ventas.components.popup]
            [ventas.components.product-listing :refer [products-list]]
            [ventas.components.cart :as cart]
            [ventas.util :as util]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]))

(defn skeleton [contents]
  (info "Rendering...")
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    [:div.bu.root
      ; [bu-debugger]
      [ventas.components.notificator/notificator]
      [ventas.components.popup/popup]
      [:div.bu.wrapper
        [ventas.themes.mariscosriasbajas.components.preheader/preheader]
        [ventas.themes.mariscosriasbajas.components.header/header]
        [sa/Container {:class "bu main"}
          [sa/Breadcrumb
            (util/interpose-fn (fn [] [sa/BreadcrumbDivider {:key (util/gen-key)}])
              (for [breadcrumb (util/breadcrumbs current-page route-params)]
                [sa/BreadcrumbSection {:key (:route breadcrumb) :href (:url breadcrumb)} (:name breadcrumb)]))]
          [sa/Divider]
          ^{:key current-page} contents]]
      [cart/sidebar]]))

(defmethod pages :frontend []
  [skeleton
    [:div
      [:h2 "Test frontend"]
      [plugin/widget :plugins.featured-products/list]
      [products-list]
      [:h3 "Test"]]])


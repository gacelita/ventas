(ns ventas.themes.mariscosriasbajas.components.skeleton
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [taoensso.timbre :as timbre :refer-macros [trace debug info warn error]]
            [fqcss.core :refer [wrap-reagent]]
            [ventas.page :refer [pages]]
            [ventas.routes :refer [route-parents routes]]
            [ventas.components.notificator :as ventas.notificator]
            [ventas.components.popup :as ventas.popup]
            [ventas.components.category-list :refer [category-list]]
            [ventas.components.product-list :refer [products-list]]
            [ventas.components.cart :as ventas.cart]
            [ventas.components.cookies :as ventas.cookies]
            [ventas.themes.mariscosriasbajas.components.header :refer [header]]
            [ventas.themes.mariscosriasbajas.components.footer :refer [footer]]
            [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
            [ventas.themes.mariscosriasbajas.components.heading :as theme.heading]
            [ventas.util :as util]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]))

(defn skeleton [contents]
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    (wrap-reagent
      [:div {:fqcss [::root]}
        [ventas.notificator/notificator]
        [ventas.popup/popup]
        [ventas.cookies/cookies
          "Esta tienda utiliza cookies y otras tecnologías para que podamos mejorar su experiencia en nuestros sitios."]
        [:div {:fqcss [::wrapper]}
          [preheader]
          [header]
          [sa/Container {:class "main"}
            [sa/Breadcrumb
              (util/interpose-fn
                (fn [] [sa/BreadcrumbDivider {:key (util/gen-key)}])
                (for [breadcrumb (util/breadcrumbs current-page route-params)]
                  [sa/BreadcrumbSection {:key (:route breadcrumb) :href (:url breadcrumb)}
                    (:name breadcrumb)]))]
            [sa/Divider]
            ^{:key current-page} contents]
          [footer]]
        [ventas.cart/sidebar]])))
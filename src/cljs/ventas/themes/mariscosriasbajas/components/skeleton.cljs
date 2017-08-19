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
            [ventas.components.menu :as ventas.menu]
            [ventas.components.breadcrumbs :as ventas.breadcrumbs]
            [ventas.themes.mariscosriasbajas.components.header :refer [header]]
            [ventas.themes.mariscosriasbajas.components.footer :refer [footer]]
            [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
            [ventas.themes.mariscosriasbajas.components.heading :as theme.heading]
            [ventas.util :as util]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]
            [ventas.routes :as routes]))

(defn skeleton [contents]
  (wrap-reagent
   [:div {:fqcss [::root]}
    [ventas.notificator/notificator]
    [ventas.popup/popup]
    [ventas.cookies/cookies
     "Esta tienda utiliza cookies y otras tecnologías para que podamos
      mejorar su experiencia en nuestros sitios."]
    [:div {:fqcss [::wrapper]}
     [preheader]
     [header]
     [ventas.menu/menu [{:text "Inicio" :href (routes/path-for :frontend)}
                        {:text "Mariscos" :href (routes/path-for :frontend.category :id 1)}]]
     [sa/Container
      [ventas.breadcrumbs/breadcrumbs]]
     [sa/Divider]
     contents
     [footer]]]))
(ns ventas.themes.mariscosriasbajas.pages.category
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [fqcss.core :refer [wrap-reagent]]
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
            [ventas.util :as util :refer [value-handler]]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]))

(defmethod pages :frontend.category []
  (reagent/with-let [data (atom {})]
    [skeleton
     (wrap-reagent
      [:div {:fqcss [::page]}
       [:div {:fqcss [::sidebar]}
        [:h2 "tast"]
        [sa/Form
         [sa/FormField
          [:input {:placeholder "Buscar"
                   :on-change (value-handler #(swap! data assoc :name %))}]]]]
       [:div {:fqcss [::content]}
        [products-list]]])]))

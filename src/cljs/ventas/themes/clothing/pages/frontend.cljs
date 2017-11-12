(ns ventas.themes.clothing.pages.frontend
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
   [ventas.components.slider :as ventas.slider]
   [ventas.plugins.slider.core :as plugins.slider]
   [ventas.themes.clothing.components.header :refer [header]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.themes.clothing.components.preheader :refer [preheader]]
   [ventas.themes.clothing.components.heading :as theme.heading]
   [ventas.themes.clothing.pages.frontend.category]
   [ventas.themes.clothing.pages.frontend.product]
   [ventas.themes.clothing.pages.frontend.privacy-policy]
   [ventas.themes.clothing.pages.frontend.login]
   [ventas.themes.clothing.pages.frontend.cart]
   [ventas.utils :as util]
   [ventas.components.base :as base]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]))

(defn page []
  [skeleton
     [:div
       [plugins.slider/slider :sample-slider]
       [base/container
         [category-list]
         [theme.heading/heading "Sugerencias de la semana"]
         [products-list]]]])

(routes/define-route!
 :frontend
 {:name (i18n ::page)
  :url ""
  :component page})


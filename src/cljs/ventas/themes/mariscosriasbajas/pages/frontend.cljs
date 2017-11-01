(ns ventas.themes.mariscosriasbajas.pages.frontend
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
   [ventas.themes.mariscosriasbajas.components.header :refer [header]]
   [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]
   [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
   [ventas.themes.mariscosriasbajas.components.heading :as theme.heading]
   [ventas.utils :as util]
   [ventas.plugin :as plugin]
   [soda-ash.core :as sa]
   [ventas.components.base :as base]))

(def ids
  [17592186045684
   17592186045682
   17592186045680])

(defmulti slides (fn [index] index))

(defmethod slides 0 [index]
  [:div.frontend-page__slide
   [base/container
    [:div.frontend-page-slide__content
     [:p.frontend-page-slide__text
      "18.10 € / ud."]
     [:h1 "Bogavante azul"]
     [:h2 "Precios a la baja, vivo o cocido (500 grs.)"]]]])

(defmethod slides 1 [index])

(defmethod slides 2 [index])

(defmethod pages :frontend []
  [skeleton
     [:div
       [ventas.slider/slider
         {:slides (map (fn [id] {:content (slides 0)
                                 :image (str "http://localhost:3450/img/" id ".jpg")})
                       ids)}]
       [base/container
         [category-list]
         [theme.heading/heading "Sugerencias de la semana"]
         [products-list]]]])


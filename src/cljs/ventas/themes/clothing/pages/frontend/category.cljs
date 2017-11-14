(ns ventas.themes.clothing.pages.frontend.category
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.product-list :refer [products-list]]
   [ventas.components.product-filters :as components.product-filters]
   [ventas.i18n :refer [i18n]]
   [ventas.page :refer [pages]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as util :refer [value-handler]]
   [ventas.routes :as routes]))

(defn page []
  (reagent/with-let [data (atom {})]
    [skeleton
     [:div.category-page.ui.container
      [:div.category-page__sidebar
       [components.product-filters/product-filters]]
      [:div.category-page__content
       [products-list]]]]))

(routes/define-route!
 :frontend.category
 {:name (fn [params]
          (js/console.log "route params" params)
          (apply i18n ::page (vals params)))
  :url ["category/" :keyword]
  :component page})
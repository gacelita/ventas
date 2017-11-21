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

(def term-counts-key ::term-counts)

(def products-key ::products)

(defn page []
  (rf/dispatch [:api/products.aggregations
                {:success-fn #(rf/dispatch [:ventas/db [term-counts-key] %])}])
  (fn []
    [skeleton
     [:div.category-page.ui.container
      [:div.category-page__sidebar
       (let [data @(rf/subscribe [:ventas/db [term-counts-key]])]
         (when (seq data)
           [components.product-filters/product-filters
            (merge data
                   {:products-path [products-key]})]))]
      [:div.category-page__content
       (let [products @(rf/subscribe [:ventas/db [products-key]])]
         [products-list products])]]]))

(routes/define-route!
 :frontend.category
 {:name (fn [params]
          (apply i18n ::page (vals params)))
  :url ["category/" :keyword]
  :component page})
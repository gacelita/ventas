(ns ventas.themes.clothing.pages.category
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.product-list :refer [products-list]]
   [ventas.i18n :refer [i18n]]
   [ventas.page :refer [pages]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as util :refer [value-handler]]))

(defmethod pages :frontend.category []
  (reagent/with-let [data (atom {})]
    [skeleton
     [:div.category-page
      [:div.category-page__sidebar
       [base/form
        [base/form-field
         [:input {:placeholder (i18n ::search)
                  :on-change (value-handler #(swap! data assoc :name %))}]]]]
      [:div.category-page__content
       [products-list]]]]))

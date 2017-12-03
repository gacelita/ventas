(ns ventas.themes.clothing.pages.frontend.product
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [ventas.page :refer [pages]]
   [ventas.utils :refer [value-handler]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.events :as events]))

(def product-key ::product)

(defn page []
  [skeleton
   (reagent/with-let [data (atom {:quantity 1})]
     [base/container
      [:div.product-page
       (let [product @(rf/subscribe [::events/db [product-key]])]
         [:div
          [:div.product-page__top
           [:div.product-page__images
            (for [image (:images product)]
              [:img {:key (:id image)
                     :src (:url image)}])]
           [:div.product-page__info
            [:h1.product-page__name
             (:name product)]
            [:h2.product-page__price
             (:price product)]
            [:p.product-page__description
             (:description product)]
            [:div.product-page__quantity
             [:button {:type "button"
                       :on-click #(swap! data update :quantity dec)}
              [base/icon {:name "minus"}]]
             [:input {:type "text"
                      :value (:quantity @data)
                      :on-change (value-handler #(swap! data assoc :quantity %))}]
             [:button {:type "button"
                       :on-click #(swap! data update :quantity inc)}
              [base/icon {:name "plus"}]]]
            [:button {:type "button"
                      :on-click #(rf/dispatch [::cart/add
                                               {:product product
                                                :quantity (:quantity @data)}])}
             [base/icon {:name "add to cart"}]
             (i18n ::add-to-cart)]]]
          [:p (:description product)]])]])])

(routes/define-route!
 :frontend.product
 {:name (i18n ::page)
  :url ["product/" :id]
  :component page})

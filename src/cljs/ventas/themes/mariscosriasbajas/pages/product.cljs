(ns ventas.themes.mariscosriasbajas.pages.product
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [fqcss.core :refer [wrap-reagent]]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.util :refer [route-param value-handler]]
            [soda-ash.core :as sa]
            [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]))

(rf/reg-sub ::product
            (fn [db _] (-> db ::product)))

(rf/reg-event-fx ::product
  (fn [cofx [_]]
    {:ws-request {:name :products/get
                  :params {:id (route-param :id)}
                  :success-fn #(rf/dispatch [:app/entity-query.next [::product] %])}}))


(defmethod pages :frontend.product []
  (rf/dispatch [::product])
  [skeleton
   (reagent/with-let [data (atom {:quantity 1})]
     (wrap-reagent
      [sa/Container
       [:div {:fqcss [::product]}
        (let [product @(rf/subscribe [::product])]
          [:div
           [:div {:fqcss [::top]}
            [:div {:fqcss [::images]}
             (for [image (:images product)]
               [:img {:key (:id image) :src (:url image)}])]
            [:div {:fqcss [::info]}
             [:h1 {:fqcss [::name]} (:name product)]
             [:h2 {:fqcss [::price]} (:price product)]
             [:p {:fqcss [::description]} (:description product)]
             [:div {:fqcss [::quantity]}
              [:button {:type "button" :on-click #(swap! data update :quantity dec)} [sa/Icon {:name "minus"}]]
              [:input {:type "text" :value (:quantity @data) :on-change (value-handler #(swap! data assoc :quantity %))}]
              [:button {:type "button" :on-click #(swap! data update :quantity inc)} [sa/Icon {:name "plus"}]]]
             [:button {:type "button"} [sa/Icon {:name "add to cart"}] "Añadir a la cesta"]]]
           [:h2 "Test frontend product" (route-param :id)]
           [:p (:description product)]
           [:pre (.stringify js/JSON (clj->js product) nil 2)]])]]))])
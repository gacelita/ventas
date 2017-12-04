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
   [ventas.events :as events]
   [ventas.events.backend :as backend]))

(def state-key ::state)

(defn- get-product-ref []
  (let [{:keys [id]} (routes/params)]
    (js/parseInt id 10)))

(defn- images-view [{:keys [product]}]
  [:div.product-page__images
   (for [image (:images product)]
     [:img {:key (:id image)
            :src (:url image)}])])

(defn- main-image-view [{:keys [product]}]
  )

(rf/reg-event-db
 ::update-quantity
 (fn [db [_ update-fn]]
   (update-in db [state-key :quantity] update-fn)))

(rf/reg-event-db
 ::set-quantity
 (fn [db [_ qty]]
   (assoc-in db [state-key :quantity] qty)))

(defn- info-view [{:keys [quantity product]}]
  (let [{:keys [name price description]} product]
    [:div.product-page__info
     [:h1.product-page__name name]
     [:h2.product-page__price price]
     [:p.product-page__description description]
     [:div.product-page__quantity
      [:button {:type "button"
                :on-click #(rf/dispatch [::update-quantity dec])}
       [base/icon {:name "minus"}]]
      [:input {:type "text"
               :value quantity
               :on-change (value-handler #(rf/dispatch [::set-quantity %]))}]
      [:button {:type "button"
                :on-click #(rf/dispatch [::update-quantity dec])}
       [base/icon {:name "plus"}]]]
     [:button {:type "button"
               :on-click #(rf/dispatch [::cart/add
                                        {:product product
                                         :quantity quantity}])}
      [base/icon {:name "add to cart"}]
      (i18n ::add-to-cart)]]))

(defn- description-view [{:keys [description]}]
  [:p description])

(defn content []
  (let [product-ref (get-product-ref)]
    (rf/dispatch [::events/db [state-key] {:quantity 1}])
    (rf/dispatch [::backend/products.get {:params {:id product-ref}
                                          :success [::events/db [state-key :product]]}])
    (fn []
      (let [state @(rf/subscribe [::events/db state-key])]
        [base/container
         [:div.product-page
          [:div.product-page__top
           [images-view state]
           [main-image-view state]
           [info-view state]]
          [description-view state]]]))))

(defn page []
  [skeleton
   [content]])

(routes/define-route!
 :frontend.product
 {:name ::page
  :url ["product/" :id]
  :component page})

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
   [ventas.events.backend :as backend]
   [ventas.components.slider :as components.slider]))

(def state-key ::state)

(defn- get-product-ref []
  (let [{:keys [id]} (routes/params)]
    (if (pos? (js/parseInt id 10))
      (js/parseInt id 10)
      (keyword id))))



(defn- images-view []
  (let [state-path [state-key :slider]]
    [:div.product-page__images
     [:div.product-page__up
      [base/icon {:name "chevron up"
                  :on-click #(rf/dispatch [::components.slider/previous state-path])}]]
     [:div.product-page__images-main
      ^{:key @(rf/subscribe [::events/db (conj state-path :render-index)])}
      [:div.product-page__images-inner {:style {:top @(rf/subscribe [::components.slider/offset state-path])}}
       (map-indexed
        (fn [idx image]
          [:img.product-page__image
           {:key idx
            :src (str "/images/" (:id image) "/resize/product-page-vertical-carousel")}])
        @(rf/subscribe [::components.slider/slides state-path]))]]
     [:div.product-page__down
      [base/icon {:name "chevron down"
                  :on-click #(rf/dispatch [::components.slider/next state-path])}]]]))

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

(rf/reg-event-db
 ::populate
 (fn [db [_ product]]
   (-> db
       (assoc-in [state-key :product] product)
       (assoc-in [state-key :slider]
                 {:slides (map (fn [image]
                                 (merge image
                                        {:width (+ 120 -6)
                                         :height (+ 190 -6)}))
                               (:images product))
                  :orientation :vertical
                  :render-index 0
                  :current-index 1}))))

(defn content []
  (let [product-ref (get-product-ref)]
    (rf/dispatch [::events/db [state-key] {:quantity 1}])
    (rf/dispatch [::backend/products.get {:params {:id product-ref}
                                          :success ::populate}])
    (fn []
      (let [state @(rf/subscribe [::events/db state-key])]
        [base/container
         [:div.product-page
          [:div.product-page__top
           [images-view]
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

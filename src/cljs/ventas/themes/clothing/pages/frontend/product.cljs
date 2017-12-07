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
   [ventas.components.slider :as components.slider]
   [ventas.utils.formatting :as formatting]))

(def state-key ::state)

(defn- get-product-ref []
  (let [{:keys [id]} (routes/params)]
    (if (pos? (js/parseInt id 10))
      (js/parseInt id 10)
      (keyword id))))

(rf/reg-event-fx
 ::set-main-image
 (fn [{:keys [db]} [_ image direction]]
   (merge {:db (assoc-in db [state-key :main-image] image)}
          (when direction
            {:dispatch
             (if (= direction :up)
               [::components.slider/previous [state-key :slider]]
               [::components.slider/next [state-key :slider]])}))))

(defn- images-view []
  (let [state-path [state-key :slider]
        {:keys [slides visible-slides]} @(rf/subscribe [::events/db state-path])]
    [:div.product-page__images
     (when (<= visible-slides (count slides))
       [:div.product-page__up
        [base/icon {:name "chevron up"
                    :on-click #(rf/dispatch [::components.slider/previous state-path])}]])
     [:div.product-page__images-wrapper
      ^{:key @(rf/subscribe [::events/db (conj state-path :render-index)])}
      [:div.product-page__images-inner {:style {:top @(rf/subscribe [::components.slider/offset state-path])}}
       (map-indexed
        (fn [idx image]
          [:img.product-page__image
           {:key idx
            :src (str "/images/" (:id image) "/resize/product-page-vertical-carousel")
            :on-click #(rf/dispatch [::set-main-image image (case idx
                                                              1 :up
                                                              visible-slides :down
                                                              nil)])}])
        @(rf/subscribe [::components.slider/slides state-path]))]]
     (when (<= visible-slides (count slides))
       [:div.product-page__down
        [base/icon {:name "chevron down"
                    :on-click #(rf/dispatch [::components.slider/next state-path])}]])]))

(defn- main-image-view [{:keys [product]}]
  (let [image @(rf/subscribe [::events/db [state-key :main-image]])]
    [:img.product-page__main-image
     {:src (str "/images/" (:id image) "/resize/product-page-main")}]))

(rf/reg-event-db
 ::update-quantity
 (fn [db [_ update-fn]]
   (update-in db [state-key :quantity] update-fn)))

(rf/reg-event-db
 ::set-quantity
 (fn [db [_ qty]]
   (assoc-in db [state-key :quantity] qty)))

(defmulti term-view (fn [{:keys [keyword]} _] keyword))

(defmethod term-view :color [_ {:keys [color name]}]
  [:div.product-page__term {:class "product-page__term--color"
                            :style {:background-color color}
                            :title name}])

(defmethod term-view :default [_ {:keys [name]}]
  [:div.product-page__term
   [:h3 name]])

(defn- info-view [{:keys [quantity product]}]
  (let [{:keys [name price description terms]} product]
    [:div.product-page__info
     [:h1.product-page__name name]
     [:p.product-page__description description]

     [:h2.product-page__price
      (let [{:keys [amount currency]} price]
        (str (formatting/format-number amount) " " (:symbol currency)))]

     [:div.product-page__terms-section
      (for [{:keys [taxonomy terms]} terms]
        [:div.product-page__taxonomy
         [:h4 (:name taxonomy)]
         [:div.product-page__terms
          (for [term terms]
            [term-view taxonomy term])]])]

     [:div.product-page__actions
      [:button.product-page__heart
       {:type "button"
        :on-click #(rf/dispatch [::cart/add {:product product
                                             :quantity quantity}])}
       [base/icon {:name "empty heart"}]]
      [:button.product-page__add-to-cart
       {:type "button"
        :on-click #(rf/dispatch [::cart/add {:product product
                                             :quantity quantity}])}
       [base/icon {:name "add to cart"}]
       (i18n ::add-to-cart)]]]))

(defn- description-view [{:keys [description]}]
  [:p description])

(rf/reg-event-db
 ::populate
 (fn [db [_ {:keys [images] :as product}]]
   (-> db
       (assoc-in [state-key :product] product)
       (assoc-in [state-key :slider]
                 (let [visible-slides 3]
                   {:slides (mapv (fn [image]
                                    (merge image
                                           {:width (+ 120 -6)
                                            :height (+ 190 -6)}))
                                  images)
                    :orientation :vertical
                    :render-index (dec (count images))
                    :current-index (if (<= visible-slides (count images))
                                     1
                                     0)
                    :visible-slides visible-slides}))
       (assoc-in [state-key :main-image] (first images)))))

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

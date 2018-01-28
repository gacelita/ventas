(ns ventas.themes.clothing.pages.frontend.product
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.common.utils :as common.utils]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.components.image :as image]
   [ventas.components.product-filters :as product-filters]
   [ventas.components.slider :as components.slider]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils.formatting :as formatting]))

(def state-key ::state)

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
       (let [current-image @(rf/subscribe [::events/db [state-key :main-image]])]
         (map-indexed
          (fn [idx image]
            [:div.product-page__image
             {:key idx
              :class (when (= (:id current-image) (:id image)) "product-page__image--active")
              :on-click #(rf/dispatch [::set-main-image image (condp = idx
                                                                1 :up
                                                                visible-slides :down
                                                                nil)])}
             [image/image (:id image) :product-page-vertical-carousel]])
          @(rf/subscribe [::components.slider/slides state-path])))]]
     (when (<= visible-slides (count slides))
       [:div.product-page__down
        [base/icon {:name "chevron down"
                    :on-click #(rf/dispatch [::components.slider/next state-path])}]])]))

(defn- mobile-images-view []
  (let [state-path [state-key :mobile-slider]
        {:keys [slides visible-slides]} @(rf/subscribe [::events/db state-path])]
    [:div.product-page__mobile-images
     (when (<= visible-slides (count slides))
       [:div.product-page__left
        [base/icon {:name "chevron left"
                    :on-click #(rf/dispatch [::components.slider/previous state-path])}]])
     [:div.product-page__images-wrapper
      ^{:key @(rf/subscribe [::events/db (conj state-path :render-index)])}
      [:div.product-page__images-inner
       (let [component-width (- @(rf/subscribe [::events/db [:window :width]]) 64)
             centered-offset (- (/ component-width 2) (/ 360 2) 48)]
         {:style {:left (+ centered-offset @(rf/subscribe [::components.slider/offset state-path]))}})
       (let [current-image @(rf/subscribe [::events/db [state-key :main-image]])]
         (map-indexed
          (fn [idx image]
            [:div.product-page__image
             {:key idx
              :class (when (= (:id current-image) (:id image)) "product-page__image--active")
              :on-click #(rf/dispatch (if (= (condp = idx
                                               1 :left
                                               visible-slides :right
                                               nil) :left)
                                        [::components.slider/previous state-path]
                                        [::components.slider/next state-path]))}
             [image/image (:id image) :product-page-horizontal-carousel]])
          @(rf/subscribe [::components.slider/slides state-path])))]]
     (when (<= visible-slides (count slides))
       [:div.product-page__right
        [base/icon {:name "chevron right"
                    :on-click #(rf/dispatch [::components.slider/next state-path])}]])]))

(defn- main-image-view [{:keys [product]}]
  (let [image @(rf/subscribe [::events/db [state-key :main-image]])]
    [:div.product-page__main-image
     [image/image (:id image) :product-page-main]]))

(rf/reg-event-db
 ::update-quantity
 (fn [db [_ update-fn]]
   (update-in db [state-key :quantity] update-fn)))

(rf/reg-event-db
 ::set-quantity
 (fn [db [_ qty]]
   (assoc-in db [state-key :quantity] qty)))

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ ref terms]]
   {:dispatch [::backend/products.get
               {:params {:id ref
                         :terms (remove nil? terms)}
                :success ::populate}]}))

(defn- get-selection-map [variation]
  (->> variation
       (map (fn [{:keys [taxonomy selected]}]
              [(:id taxonomy) (:id selected)]))
       (into {})))

(rf/reg-event-fx
 ::select-term
 (fn [{:keys [db]} [_ ref {taxonomy-id :id} {term-id :id}]]
   {:pre [taxonomy-id term-id]}
   (let [variation (get-in db [state-key :product :variation])
         selection-map (-> (get-selection-map variation)
                           (assoc taxonomy-id term-id))]
     {:dispatch [::fetch ref (vals selection-map)]})))

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
       (assoc-in [state-key :mobile-slider]
                 (let [visible-slides 2]
                   {:slides (mapv (fn [image]
                                    (merge image
                                           {:width (+ 360 -6)
                                            :height (+ 540 -6)}))
                                  images)
                    :orientation :horizontal
                    :render-index (dec (count images))
                    :current-index (if (<= visible-slides (count images))
                                     1
                                     0)
                    :visible-slides visible-slides}))
       (assoc-in [state-key :main-image] (first images)))))

(defmulti term-view (fn [{:keys [keyword]} _] keyword))

(defmethod term-view :color [taxonomy {:keys [color name] :as term} active?]
  [product-filters/color-term
   term
   {:active? active?
    :on-click #(rf/dispatch [::select-term
                             (routes/ref-from-param :id)
                             taxonomy
                             term])}])

(defmethod term-view :default [taxonomy {:keys [name] :as term} active?]
  [:div.product-page__term {:class (when active? "product-page__term--active")
                            :on-click #(rf/dispatch [::select-term
                                                     (routes/ref-from-param :id)
                                                     taxonomy
                                                     term])}
   [:h3 name]])

(defn- info-view [_]
  (rf/dispatch [::events/users.favorites.list])
  (fn [{:keys [quantity product]}]
    (let [{:keys [name price description variation]} product]
      [:div.product-page__info
       [:h1.product-page__name name]
       [:p.product-page__description description]

       [:h2.product-page__price
        (let [{:keys [value currency]} price]
          (str (formatting/format-number value) " " (:symbol currency)))]

       [:div.product-page__terms-section
        (for [{:keys [taxonomy terms selected]} variation]
          [:div.product-page__taxonomy
           [:h4 (str (:name taxonomy) ": "
                     (:name selected))]
           [:div.product-page__terms
            (for [term terms]
              [term-view taxonomy term (= term selected)])]])]

       [:div.product-page__actions
        (let [favorites (set @(rf/subscribe [::events/db :users.favorites]))]
          [:button.product-page__heart
           {:type "button"
            :class (when (contains? favorites (:id product))
                     "product-page__heart--active")
            :on-click #(rf/dispatch [::events/users.favorites.toggle (:id product)])}
           [base/icon {:name "empty heart"}]])
        [:button.product-page__add-to-cart
         {:type "button"
          :on-click #(rf/dispatch [::cart/add (:id product)])}
         [base/icon {:name "add to cart"}]
         (i18n ::add-to-cart)]]])))

(defn- description-view [{{:keys [details]} :product}]
  (when-not (empty? details)
    [:div.product-page__details
     [base/container
      [:h2 (i18n ::product-details)]
      [:div.product-page__details-inner
       [:p details]]]]))

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (assoc db state-key {:quantity 1})
    :dispatch [::fetch (routes/ref-from-param :id)]}))

(defn content []
  (let [state @(rf/subscribe [::events/db state-key])]
    [:div.product-page
     [mobile-images-view]
     [base/container
      [:div.product-page__top
       [images-view]
       [main-image-view state]
       [info-view state]]]
     [:div.product-page__bottom
      [description-view state]]]))

(defn page []
  [skeleton
   [content]])

(routes/define-route!
 :frontend.product
 {:name ::page
  :url ["product/" :id]
  :component page
  :init-fx [::init]})

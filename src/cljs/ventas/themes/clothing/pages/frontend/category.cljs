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
   [ventas.routes :as routes]
   [ventas.components.infinite-scroll :as scroll]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(def term-counts-key ::term-counts)

(def products-key ::products)

(def state-key ::state)

(defn- get-ref []
  (let [{:keys [id]} (routes/params)]
    (if (pos? (js/parseInt id 10))
      (js/parseInt id 10)
      (keyword id))))

(rf/reg-event-db
 ::populate
 (fn [db [_ category]]
   (assoc-in db [state-key :category] category)))

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ ref]]
   {:dispatch [::backend/categories.get {:params {:id ref}
                                         :success ::populate}]}))

(defn content [{:keys [ref]}]
  (rf/dispatch [::fetch ref])
  (rf/dispatch [::backend/products.aggregations
                {:success #(rf/dispatch [::events/db [term-counts-key] %])}])
  (fn [{:keys [ref]}]
    [:div.category-page.ui.container
     [:div.category-page__sidebar
      (let [data @(rf/subscribe [::events/db [term-counts-key]])]
        (when (seq data)
          [components.product-filters/product-filters
           (merge data
                  {:products-path [products-key]})]))]
     [:div.category-page__content
      (let [products @(rf/subscribe [::events/db [products-key :products]])]
        [products-list products])
      [scroll/infinite-scroll
       (let [more-items-available? true]
         {:can-show-more? more-items-available?
          :load-fn #(rf/dispatch [::components.product-filters/apply-filters [products-key]])})]]]))

(defn- page []
  [skeleton
   [content
    (let [ref (get-ref)]
      {:key (hash ref)
       :ref ref})]])

(rf/reg-sub
 ::title
 (fn [db _]
   (let [{:keys [category]} (get db state-key)]
     (:name category))))

(routes/define-route!
 :frontend.category
 {:name [::title]
  :url ["category/" :id]
  :component page})
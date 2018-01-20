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
   [ventas.utils :as utils]
   [ventas.routes :as routes]
   [ventas.components.infinite-scroll :as scroll]
   [ventas.events.backend :as backend]
   [ventas.events :as events]
   [clojure.string :as str]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (assoc-in db [state-key :filters :categories] [(routes/ref-from-param :id)])
    :dispatch [::fetch]}))

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} _]
   {:dispatch [::backend/products.aggregations
               {:success ::fetch.next
                :params (get-in db [state-key :filters])}]}))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ {:keys [items taxonomies]}]]
   (-> db
       (assoc-in [state-key :items] items)
       (assoc-in [state-key :taxonomies] taxonomies))))

(rf/reg-event-fx
 ::update-filters
 (fn [{:keys [db]} [_ f]]
   {:db (update-in db [state-key :filters] f)
    :dispatch [::fetch]}))

(defn content []
  (rf/dispatch [::init])
  (fn [_]
    [:div.category-page.ui.container

     [:div.category-page__sidebar
      (let [{:keys [filters taxonomies]} @(rf/subscribe [::events/db [state-key]])]
        [components.product-filters/product-filters
         {:filters filters
          :taxonomies taxonomies
          :event ::update-filters}])]

     [:div.category-page__content
      (let [products @(rf/subscribe [::events/db [state-key :items]])]

        [products-list products])
      #_[scroll/infinite-scroll
       (let [more-items-available? true]
         {:can-show-more? more-items-available?
          :load-fn #(rf/dispatch [::components.product-filters/apply-filters [state-key]])})]]]))

(defn- page []
  [skeleton
   ^{:key (hash (routes/ref-from-param :id))} [content]])

(rf/reg-sub
 ::title
 (fn [db _]
   (let [{:keys [category brand]} (get-in db [state-key :filters])]
     (or (:name category)
         (:brand category)
         (i18n ::search)))))

(routes/define-route!
 :frontend.category
 {:name [::title]
  :url ["category/" :id]
  :component page})
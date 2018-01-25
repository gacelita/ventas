(ns ventas.themes.clothing.pages.frontend.category
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.error :as error]
   [ventas.components.infinite-scroll :as scroll]
   [ventas.components.product-filters :as components.product-filters]
   [ventas.components.product-list :refer [products-list]]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.page :refer [pages]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as utils]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (if-let [ref (routes/ref-from-param :id)]
          (assoc-in db [state-key :filters :categories] [ref])
          db)
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
  [:div.category-page.ui.container

   (let [{:keys [filters taxonomies items]} @(rf/subscribe [::events/db [state-key]])]
     (if (and items (empty? items))
       [error/no-data]
       (list
        ^{:key :sidebar}
        [:div.category-page__sidebar
         [components.product-filters/product-filters
          {:filters filters
           :taxonomies taxonomies
           :event ::update-filters}]]

        ^{:key :content}
        [:div.category-page__content
         [products-list items]
         #_[scroll/infinite-scroll
            (let [more-items-available? true]
              {:can-show-more? more-items-available?
               :load-fn #(rf/dispatch [::components.product-filters/apply-filters [state-key]])})]])))])

(defn- page []
  [skeleton [content]])

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
  :component page
  :init-fx [::init]})

(routes/define-route!
 :frontend.search
 {:name [::title]
  :url "search"
  :component page
  :init-fx [::init]})
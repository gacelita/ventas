(ns ventas.themes.clothing.pages.frontend.category
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.error :as error]
   [ventas.components.infinite-scroll :as scroll]
   [ventas.components.product-filters :as components.product-filters]
   [ventas.components.product-list :as components.product-list]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.utils :as utils]))

(def state-key ::state)

(rf/reg-event-fx
 ::init
 (fn [{:keys [db]} _]
   {:db (let [ref (routes/ref-from-param :id)
              search (:search (routes/params))
              db (-> db
                     (assoc-in [state-key :filters] {})
                     (assoc-in [state-key :pagination] {}))]
          (cond
            ref (assoc-in db [state-key :filters :categories] [ref])
            search (assoc-in db [state-key :filters :name] search)
            :default db))
    :dispatch [::fetch]}))

(rf/reg-event-fx
 ::fetch
 (fn [{:keys [db]} [_ append?]]
   {:dispatch [::backend/products.aggregations
               {:success [::fetch.next append?]
                :params {:filters (get-in db [state-key :filters])
                         :pagination (merge {:page 0
                                             :items-per-page 4
                                             :sorting {:field :price
                                                       :direction :asc}}
                                            (get-in db [state-key :pagination]))}}]}))

(rf/reg-event-db
 ::fetch.next
 (fn [db [_ append? result]]
   (-> db
       (update state-key
               #(merge % (select-keys result [:can-load-more? :taxonomies])))
       (update-in [state-key :items]
                  #(if append?
                     (concat % (:items result))
                     (:items result))))))

(rf/reg-event-fx
 ::update-filters
 (fn [{:keys [db]} [_ f]]
   {:db (-> db
            (update-in [state-key :filters] f)
            (assoc-in [state-key :pagination :page] 0))
    :dispatch [::fetch]}))

(rf/reg-event-fx
 ::set-sorting
 (fn [{:keys [db]} [_ sorting]]
   {:db (-> db
            (assoc-in [state-key :pagination :sorting] sorting)
            (assoc-in [state-key :pagination :page] 0))
    :dispatch [::fetch]}))

(rf/reg-event-fx
 ::next-page
 (fn [{:keys [db]} _]
   {:db (update-in db [state-key :pagination :page] inc)
    :dispatch [::fetch true]}))

(defn- find-category [slug]
  (->> @(rf/subscribe [::events/db :categories])
       (filter (fn [category]
                 (= slug (:slug category))))
       (first)))

(defn header [filters]
  [:div.category-page__header
   [base/header {:as "h2"}
    (if (:name filters)
      (str "Searching for: \"" (:name filters) "\"")
      (str "Browsing category: \""
           (let [slug (first (:categories filters))]
             (:name (find-category slug)))
           "\""))]
   [base/dropdown
    (let [options [{:value "lowest-price"
                    :text (i18n ::lowest-price)
                    :sorting {:field :price
                              :direction :asc}}
                   {:value "highest-price"
                    :sorting {:field :price
                              :direction :desc}
                    :text (i18n ::highest-price)}]]
      {:selection true
       :options options
       :default-value "lowest-price"
       :on-change #(rf/dispatch [::set-sorting (->> options
                                                    (filter (fn [option]
                                                              (= (:value option) (.-value %2))))
                                                    first
                                                    :sorting)])})]])

(defn content []
  (let [{:keys [filters taxonomies items can-load-more?]} @(rf/subscribe [::events/db [state-key]])]
    [:div.category-page.ui.container
     [:div.category-page__sidebar
      [components.product-filters/product-filters
       {:filters filters
        :taxonomies taxonomies
        :event ::update-filters}]]

     [:div.category-page__content
      [header filters]
      (if (and items (empty? items))
        [error/no-data]
        [components.product-list/product-list items])
      [scroll/infinite-scroll
       {:can-show-more? can-load-more?
        :load-fn #(rf/dispatch [::next-page])}]]]))

(defn- page []
  [skeleton [content]])

(rf/reg-sub
 ::title
 (fn [db _]
   (let [{:keys [categories brand]} (get-in db [state-key :filters])
         category (find-category (first categories))]
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
   :url ["search/" :search]
   :component page
   :init-fx [::init]})

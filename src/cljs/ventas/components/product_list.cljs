(ns ventas.components.product-list
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [fqcss.core :refer [wrap-reagent]]
            [cljs.pprint :refer [pprint]]
            [ventas.routes :as routes]))

(rf/reg-sub :components/product-list
  (fn [db _] (-> db :components/product-list)))

(rf/reg-event-fx :components/product-list
  (fn [cofx [_]]
    {:ws-request {:name :products/list
                  :success-fn #(rf/dispatch [:app/entity-query.next [:components/product-list] %])}}))

(defn products-list []
  "Products list"
  (rf/dispatch [:components/product-list])
  (fn []
    (wrap-reagent
     [:div {:fqcss [::list]}
      (for [product @(rf/subscribe [:components/product-list])]
        [:div {:fqcss [::product] :key (:id product)}
         (when (seq (:images product))
           [:img {:src (:url (first (:images product)))}])
         [:div {:fqcss [::content]}
          [:a {:href (routes/path-for :frontend.product :id (:id product))} (:name product)]
          [:div {:fqcss [::price]}
           [:span (util/format-price (:price product))]]]])])))
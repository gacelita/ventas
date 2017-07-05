(ns ventas.components.category-list
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [fqcss.core :refer [wrap-reagent]]
            [cljs.pprint :refer [pprint]]
            [ventas.routes :as routes]))

(def component-kw ::product-list)

(rf/reg-sub component-kw
  (fn [db _] (-> db component-kw)))

(rf/reg-event-fx component-kw
  (fn [cofx [_]]
    {:ws-request {:name :categories/list
                  :success-fn #(rf/dispatch [:app/entity-query.next [component-kw] %])}}))

(defn category-list []
  "Category list"
  (rf/dispatch [component-kw])
  (fn []
    (wrap-reagent
     [:div {:fqcss [::list]}
      (for [category @(rf/subscribe [component-kw])]
        [:div {:fqcss [::category] :on-click #(routes/go-to :frontend.category :id 1)}
         (when (:image category)
           [:img {:fqcss [::image] :src (:url (:image category))}])
         [:div {:fqcss [::content]}
          [:h3 (:name category)]
          (when (:description category)
            [:p (:description category)])]])])))
(ns ventas.themes.mariscosriasbajas.pages.product
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.util :refer [route-param]]
            [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]))

(rf/reg-sub ::product
            (fn [db _] (-> db ::product)))

(rf/reg-event-fx ::product
  (fn [cofx [_]]
    {:ws-request {:name :products/get
                  :params {:id (route-param :id)}
                  :success-fn #(rf/dispatch [:app/entity-query.next [::product] %])}}))


(defmethod pages :frontend.product []
  (rf/dispatch [::product])
  [skeleton
    [:div
      [:h2 "Test frontend product" (route-param :id)]
      [:pre (.stringify js/JSON (clj->js @(rf/subscribe [::product])) nil 2)]]])
(ns ventas.pages.frontend.product
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.pages.frontend :as frontend]
            [ventas.util :refer [route-param]]))

(rf/reg-sub :pages.product/product
            (fn [db _] (-> db :pages.product/product)))

(rf/reg-event-fx :pages.product/product
  (fn [cofx [_]]
    {:ws-request {:name :products/get
                  :params {:id (route-param :id)}
                  :success-fn #(rf/dispatch [:app/entity-query.next [:pages.product/product] %])}}))


(defmethod pages :frontend.product []
  (rf/dispatch [:pages.product/product])
  [frontend/skeleton
    [:div
      [:h2 "Test frontend product" (route-param :id)]
      [:pre (.stringify js/JSON (clj->js @(rf/subscribe [:pages.product/product])) nil 2)]]])
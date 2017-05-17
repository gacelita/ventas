(ns ventas.components.cart
  (:require [ventas.util :as util :refer [go-to]]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [bidi.bidi :as bidi]
            [ventas.routes :refer [routes]]))

(rf/reg-sub :components/cart
  (fn [db _] (-> db :components/cart)))

(rf/reg-event-fx :components/cart
  (fn [cofx [_]]
    {:ws-request {:name :products/list
                  :success-fn #(rf/dispatch [:app/entity-query.next [:components/product-list] %])}}))

(defn cart []
  "Cart"
  (rf/dispatch [:components/cart])
  (fn []
    [:div.bu.product-list
     (for [product @(rf/subscribe [:components/product-list])]
       [:div.bu.product-listing
        [:a {:href (bidi/path-for routes :frontend.product :id (:id product)) } (:name product)]
        (for [image (:images product)]
          [:img {:src (:url image)}])])]))
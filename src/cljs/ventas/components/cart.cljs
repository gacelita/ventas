(ns ventas.components.cart
  (:require [ventas.util :as util :refer [go-to]]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [soda-ash.core :as sa]
            [bidi.bidi :as bidi]
            [ventas.routes :refer [routes]]
            [ventas.local-storage :as storage]))

(rf/reg-sub :components/cart
  (fn [db _] (-> db :components/cart)))

(rf/reg-event-fx :components/cart
  (fn [cofx [_]]
    {:db (assoc-in cofx [:db :cart] (get-in cofx [:local-storage :cart]))}))

(defn sidebar []
  "Cart"
  (rf/dispatch [:components/cart])
  (fn []
    [:div.ventas.cart-sidebar
     [:pre (.stringify js/JSON (clj->js @(rf/subscribe [:components/cart])) nil 2)]]))
(ns ventas.devcards.cart
  (:require
   [devcards.core :refer-macros [defcard-rg]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.utils.debug :as debug]))

(defn pprint-subscription [sub]
  [:pre (with-out-str (cljs.pprint/pprint @sub))])

(defn cart-wrapper []
  (reagent/with-let [hover-id (gensym)
                     visible (reagent/atom true)]
    [:div
     [base/button
      {:on-click #(reset! visible (not @visible))}
      "Toggle cart hover"]
     [debug/pprint-sub (rf/subscribe [::cart/main])]]))

(defcard-rg cart
  "Regular cart"
  [cart-wrapper])

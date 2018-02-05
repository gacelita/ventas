(ns ventas.devcards.cart
  (:require
   [reagent.core :as reagent]
   [devcards.core]
   [ventas.components.cart :as cart]
   [ventas.components.base :as base]
   [ventas.utils.debug :as debug]
   [re-frame.core :as rf])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn pprint-subscription [sub]
  [:pre (with-out-str (cljs.pprint/pprint @sub))])

(defn cart-wrapper []
  (reagent/with-let [hover-id (gensym)
                     visible (reagent/atom true)]
    [:div
     [base/button
      {:on-click #(reset! visible (not @visible))}
      "Toggle cart hover"]
     [debug/pprint-sub (rf/subscribe [:ventas.components.cart/main])]]))

(defcard-rg cart
  "Regular cart"
  [cart-wrapper])

(ns ventas.devcards.cart
  (:require
   [reagent.core :as reagent]
   [devcards.core]
   [ventas.components.cart :as components.cart]
   [ventas.components.base :as base]
   [ventas.utils.debug :as debug]
   [re-frame.core :as rf])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn pprint-subscription [sub]
  [:pre (with-out-str (cljs.pprint/pprint @sub))])

(defn- add-product []
  (rf/dispatch [:ventas.components.cart/add {:id (do (gensym) @gensym_counter)
                                             :name (random-uuid)
                                             :quantity 1}]))

(defn cart-wrapper []
  (reagent/with-let [hover-id (gensym)
                     visible (reagent/atom true)]
    [:div
     [base/button {:on-click add-product}
      "Add product"]
     [base/button
      {:on-click #(reset! visible (not @visible))}
      "Toggle cart hover"]
     [debug/pprint-sub (rf/subscribe [:ventas.components.cart/main])]
     [:div {:style {:position "relative"}}
      [components.cart/hover hover-id {:visible @visible}]]
     ]))

(defcard-rg cart
  "Regular cart"
  [cart-wrapper])

(defonce init (dotimes [n 4] (add-product)))
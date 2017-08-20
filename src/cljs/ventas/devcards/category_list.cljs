(ns ventas.devcards.category-list
  (:require
   [reagent.core :as reagent]
   [devcards.core]
   [ventas.components.category-list :as components.category-list]
   [re-frame.core :as rf]
   [ventas.utils.debug :as debug])
  (:require-macros
   [devcards.core :refer [defcard-rg]]))

(defn add-category []
  (rf/dispatch [:ventas.components.category-list/add
                {:id (do (gensym) @gensym_counter)
                 :name (random-uuid)
                 :description "A sample description"}]))

(defcard-rg regular-category-list
            "Regular category list"
            [:div
             [components.category-list/category-list]
             [debug/pprint-sub (rf/subscribe [:ventas.components.category-list/main])]])

(defonce init (defonce init (dotimes [n 4] (add-category))))
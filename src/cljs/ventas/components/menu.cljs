(ns ventas.components.menu
  (:require
   [ventas.components.base :as base]))

(defn menu-children [children]
  [:div.menu__children
   (for [{:keys [id text href] :as child} children]
     ^{:key (or id (hash child))}
     [:a.menu__child {:href href} text])])

(defn menu-item [{:keys [href text children]} current?]
  [:li.menu__item
   {:class (when current? "menu__item--active")}
   [:a {:href href} text]
   (when children
     [:div.menu__overlay
      [base/container
       [menu-children children]]])])

(defn menu [{:keys [current-fn items]}]
  [:div.menu
   [base/container
    [:ul.menu__items
     (doall
      (for [item items]
        ^{:key (or (:id item) (hash item))}
        [menu-item item (current-fn item)]))]]])

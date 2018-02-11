(ns ventas.components.menu
  (:require
   [ventas.components.base :as base]))

(defn menu-children [children]
  [:div.menu__children
   (for [{:keys [id text href] :as child} children]
     ^{:key (or id (hash child))}
     [:a.menu__child {:href href} text])])

(defn menu-item [current {:keys [href text id children]}]
  [:li.menu__item
   {:class (when (= id current) "menu__item--active")}
   [:a {:href href} text]
   (when children
     [:div.menu__overlay
      [base/container
       [menu-children children]]])])

(defn menu [{:keys [current items]}]
  [:div.menu
   [base/container
    [:ul.menu__items
     (for [item items]
       ^{:key (or (:id item) (hash item))}
       [menu-item current item])]]])
